package edu.alibaba.mpc4j.s2pc.aby.main.trun;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncParty;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.ZlTruncFactory.ZlTruncType;

/**
 * Zl truncation main.
 *
 * @author Liqiang Peng
 * @date 2023/10/12
 */
public class ZlTruncMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlTruncMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "ZL_TRUNC_TASK";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 16;
    /**
     * warmup shift bit
     */
    private static final int WARMUP_SHIFT_BIT = 1;
    /**
     * warmup set size
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * properties
     */
    private final Properties properties;

    public ZlTruncMain(Properties properties) {
        this.properties = properties;
        stopWatch = new StopWatch();
    }

    public void run() throws Exception {
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        String zlTruncTypeString = PropertiesUtils.readString(properties, "pto_name");
        ZlTruncType zlTruncType = ZlTruncType.valueOf(zlTruncTypeString);
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        int s = PropertiesUtils.readInt(properties, "shift_bit");
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int setSizeNum = logSetSizes.length;
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        ZlTruncConfig config = ZlTruncConfigUtils.createZlTruncConfig(properties);
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = zlTruncType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tSet Size\tIs Parallel\tThread Num\tSilent"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        serverRpc.connect();
        int taskId = 0;
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // not parallel
            runServer(
                serverRpc, clientParty, config, taskId++, setSizes[setSizeIndex], s, elementBitLength, false, printWriter
            );
            // parallel
            runServer(
                serverRpc, clientParty, config, taskId++, setSizes[setSizeIndex], s, elementBitLength, true, printWriter
            );
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, ZlTruncConfig config, int taskId) throws Exception {
        ZlTruncParty sender = ZlTruncFactory.createSender(serverRpc, clientParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(false);
        SecureRandom secureRandom = new SecureRandom();
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, WARMUP_ELEMENT_BIT_LENGTH);
        BigInteger[] xi = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> new BigInteger(WARMUP_ELEMENT_BIT_LENGTH, secureRandom))
            .toArray(BigInteger[]::new);
        sender.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", sender.ownParty().getPartyName());
        sender.init(WARMUP_ELEMENT_BIT_LENGTH, WARMUP_SET_SIZE);
        sender.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", sender.ownParty().getPartyName());
        sender.trunc(SquareZlVector.create(zl, xi, false), WARMUP_SHIFT_BIT);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        sender.destroy();
        LOGGER.info("(warmup) {} finish", sender.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, ZlTruncConfig config, int taskId,
                           int num, int s, int elementBitLength, boolean parallel, PrintWriter printWriter)
        throws MpcAbortException {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: num = {}, bit-length = {}, s = {}, parallel = {}, silent = {}",
            serverRpc.ownParty().getPartyName(), num, elementBitLength, s, parallel, silent
        );
        ZlTruncParty sender = ZlTruncFactory.createSender(serverRpc, clientParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(parallel);
        SecureRandom secureRandom = new SecureRandom();
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, elementBitLength);
        BigInteger[] xi = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(elementBitLength, secureRandom))
            .toArray(BigInteger[]::new);
        SquareZlVector squareZlVector = SquareZlVector.create(zl, xi, false);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        LOGGER.info("{} init", sender.ownParty().getPartyName());
        stopWatch.start();
        sender.init(elementBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = sender.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = sender.getRpc().getPayloadByteLength();
        long initSendByteLength = sender.getRpc().getSendByteLength();
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        LOGGER.info("{} execute", sender.ownParty().getPartyName());
        stopWatch.start();
        sender.trunc(squareZlVector, s);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = sender.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = sender.getRpc().getPayloadByteLength();
        long ptoSendByteLength = sender.getRpc().getSendByteLength();
        String info = sender.ownParty().getPartyId()
            + "\t" + num
            + "\t" + sender.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        sender.destroy();
        LOGGER.info("{} finish", sender.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        String zlTruncTypeString = PropertiesUtils.readString(properties, "pto_name");
        ZlTruncType zlTruncType = ZlTruncType.valueOf(zlTruncTypeString);
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        int s = PropertiesUtils.readInt(properties, "shift_bit");
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int setSizeNum = logSetSizes.length;
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        ZlTruncConfig config = ZlTruncConfigUtils.createZlTruncConfig(properties);
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = zlTruncType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\t Set Size\tIs Parallel\tThread Num\tSilent"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // not parallel
            runClient(clientRpc, serverParty, config, taskId++, setSizes[setSizeIndex], s, elementBitLength, false, printWriter);
            runClient(clientRpc, serverParty, config, taskId++, setSizes[setSizeIndex], s, elementBitLength, true, printWriter);
        }
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, ZlTruncConfig config, int taskId) throws Exception {
        ZlTruncParty receiver = ZlTruncFactory.createReceiver(clientRpc, serverParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(false);
        SecureRandom secureRandom = new SecureRandom();
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, WARMUP_ELEMENT_BIT_LENGTH);
        BigInteger[] xi = IntStream.range(0, WARMUP_SET_SIZE)
            .mapToObj(i -> new BigInteger(WARMUP_ELEMENT_BIT_LENGTH, secureRandom))
            .toArray(BigInteger[]::new);
        receiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", receiver.ownParty().getPartyName());
        receiver.init(WARMUP_ELEMENT_BIT_LENGTH, WARMUP_SET_SIZE);
        receiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", receiver.ownParty().getPartyName());
        receiver.trunc(SquareZlVector.create(zl, xi, false), WARMUP_SHIFT_BIT);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        receiver.destroy();
        LOGGER.info("(warmup) {} finish", receiver.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, ZlTruncConfig config, int taskId,
                           int num, int s, int elementBitLength, boolean parallel, PrintWriter printWriter)
        throws MpcAbortException {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        LOGGER.info(
            "{}: num = {}, bit-length = {}, s = {}, parallel = {}, silent = {}",
            clientRpc.ownParty().getPartyName(), num, elementBitLength, s, parallel, silent
        );
        ZlTruncParty receiver = ZlTruncFactory.createReceiver(clientRpc, serverParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(parallel);
        SecureRandom secureRandom = new SecureRandom();
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, elementBitLength);
        BigInteger[] xi = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(elementBitLength, secureRandom))
            .toArray(BigInteger[]::new);
        SquareZlVector squareZlVector = SquareZlVector.create(zl, xi, false);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        LOGGER.info("{} init", receiver.ownParty().getPartyName());
        stopWatch.start();
        receiver.init(elementBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = receiver.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = receiver.getRpc().getPayloadByteLength();
        long initSendByteLength = receiver.getRpc().getSendByteLength();
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        LOGGER.info("{} execute", receiver.ownParty().getPartyName());
        stopWatch.start();
        receiver.trunc(squareZlVector, s);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = receiver.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = receiver.getRpc().getPayloadByteLength();
        long ptoSendByteLength = receiver.getRpc().getSendByteLength();
        String info = receiver.ownParty().getPartyId()
            + "\t" + num
            + "\t" + receiver.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        receiver.destroy();
        LOGGER.info("{} finish", receiver.ownParty().getPartyName());
    }
}

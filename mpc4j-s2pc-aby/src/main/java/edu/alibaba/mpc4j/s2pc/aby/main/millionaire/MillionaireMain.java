package edu.alibaba.mpc4j.s2pc.aby.main.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireParty;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory.createSender;

/**
 * Millionaire main.
 *
 * @author Liqiang Peng
 * @date 2023/10/12
 */
public class MillionaireMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(MillionaireMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "MILLIONAIRE_TASK";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 32;
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

    public MillionaireMain(Properties properties) {
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
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int setSizeNum = logSetSizes.length;
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        MillionaireConfig config = MillionaireConfigUtils.createMillionaireConfig(properties);
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = TASK_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tSet Size\tIs Parallel\tThread Num"
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
                serverRpc, clientParty, config, taskId++, setSizes[setSizeIndex], elementBitLength, false, printWriter
            );
            // parallel
            runServer(
                serverRpc, clientParty, config, taskId++, setSizes[setSizeIndex], elementBitLength, true, printWriter
            );
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, MillionaireConfig config, int taskId) throws Exception {
        MillionaireParty sender = createSender(serverRpc, clientParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(false);
        SecureRandom secureRandom = new SecureRandom();
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        byte[][] xi = new byte[WARMUP_SET_SIZE][byteL];
        for (int i = 0; i < WARMUP_SET_SIZE; i++) {
            secureRandom.nextBytes(xi[i]);
            BytesUtils.reduceByteArray(xi[i], WARMUP_ELEMENT_BIT_LENGTH);
        }
        sender.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", sender.ownParty().getPartyName());
        sender.init(WARMUP_ELEMENT_BIT_LENGTH, WARMUP_SET_SIZE);
        sender.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", sender.ownParty().getPartyName());
        sender.lt(WARMUP_ELEMENT_BIT_LENGTH, xi);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        sender.destroy();
        LOGGER.info("(warmup) {} finish", sender.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, MillionaireConfig config, int taskId,
                           int num, int elementBitLength, boolean parallel, PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: num = {}, bit-length = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), num, elementBitLength, parallel
        );
        MillionaireParty sender = createSender(serverRpc, clientParty, config);
        sender.setTaskId(taskId);
        sender.setParallel(parallel);
        SecureRandom secureRandom = new SecureRandom();
        int byteL = CommonUtils.getByteLength(elementBitLength);
        byte[][] xi = new byte[num][byteL];
        for (int i = 0; i < elementBitLength; i++) {
            secureRandom.nextBytes(xi[i]);
            BytesUtils.reduceByteArray(xi[i], elementBitLength);
        }
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
        sender.lt(elementBitLength, xi);
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
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        sender.getRpc().synchronize();
        sender.getRpc().reset();
        sender.destroy();
        LOGGER.info("{} finish", sender.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int setSizeNum = logSetSizes.length;
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        MillionaireConfig config = MillionaireConfigUtils.createMillionaireConfig(properties);
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = TASK_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\t Set Size\tIs Parallel\tThread Num"
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
            runClient(clientRpc, serverParty, config, taskId++, setSizes[setSizeIndex], elementBitLength, false, printWriter);
            runClient(clientRpc, serverParty, config, taskId++, setSizes[setSizeIndex], elementBitLength, true, printWriter);
        }
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, MillionaireConfig config, int taskId) throws Exception {
        MillionaireParty receiver = MillionaireFactory.createReceiver(clientRpc, serverParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(false);
        SecureRandom secureRandom = new SecureRandom();
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        byte[][] xi = new byte[WARMUP_SET_SIZE][byteL];
        for (int i = 0; i < WARMUP_SET_SIZE; i++) {
            secureRandom.nextBytes(xi[i]);
            BytesUtils.reduceByteArray(xi[i], WARMUP_ELEMENT_BIT_LENGTH);
        }
        receiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", receiver.ownParty().getPartyName());
        receiver.init(WARMUP_ELEMENT_BIT_LENGTH, WARMUP_SET_SIZE);
        receiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", receiver.ownParty().getPartyName());
        receiver.lt(WARMUP_ELEMENT_BIT_LENGTH, xi);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        receiver.destroy();
        LOGGER.info("(warmup) {} finish", receiver.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, MillionaireConfig config, int taskId,
                           int num, int elementBitLength, boolean parallel, PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: num = {}, bit-length = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), num, elementBitLength, parallel
        );
        MillionaireParty receiver = MillionaireFactory.createReceiver(clientRpc, serverParty, config);
        receiver.setTaskId(taskId);
        receiver.setParallel(parallel);
        SecureRandom secureRandom = new SecureRandom();
        int byteL = CommonUtils.getByteLength(elementBitLength);
        byte[][] xi = new byte[num][byteL];
        for (int i = 0; i < num; i++) {
            secureRandom.nextBytes(xi[i]);
            BytesUtils.reduceByteArray(xi[i], elementBitLength);
        }
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
        receiver.lt(elementBitLength, xi);
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
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        receiver.getRpc().synchronize();
        receiver.getRpc().reset();
        receiver.destroy();
        LOGGER.info("{} finish", receiver.ownParty().getPartyName());
    }
}

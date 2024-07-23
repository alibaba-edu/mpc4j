package edu.alibaba.mpc4j.s2pc.aby.main.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * OSN main.
 *
 * @author Feng Han
 * @date 2024/7/9
 */
public class RosnMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosnMain.class);
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "rosn_pto_name";
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "ROSN";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup set size
     */
    private static final int WARMUP_DATA_SIZE = 1 << 10;
    /**
     * < [size, byteLen], ... >
     */
    private final List<int[]> parameters;
    /**
     * random osn config
     */
    private final RosnConfig rosnConfig;

    public RosnMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PSI config
        LOGGER.info("{} read OSN config", ownRpc.ownParty().getPartyName());
        rosnConfig = RosnConfigUtils.createConfig(properties);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        // mode code, &01 > 0: fixed total byte size; &10 > 0: fixed data size or payload byte length
        int modeCode = PropertiesUtils.readInt(properties, "mode_code");
        parameters = new LinkedList<>();
        int[] logDataSize = PropertiesUtils.readLogIntArray(properties, "log_data_size");
        int[] dataSizes = Arrays.stream(logDataSize).map(each -> 1 << each).toArray();
        if ((modeCode & 1) > 0) {
            int[] logByteLens = PropertiesUtils.readIntArray(properties, "log_total_byte_length");
            long[] totalByteLens = Arrays.stream(logByteLens).mapToLong(each -> {
                MathPreconditions.checkGreater("logByteLen", each, 4);
                return 1L << each;
            }).toArray();
            for (long totalByteLen : totalByteLens) {
                for (int dataSize : dataSizes) {
                    long payloadByteLen = totalByteLen / dataSize;
                    if (payloadByteLen >= CommonConstants.BLOCK_BYTE_LENGTH && payloadByteLen <= Integer.MAX_VALUE) {
                        parameters.add(new int[]{dataSize, (int) payloadByteLen});
                    }
                }
            }
        }
        if ((modeCode & 2) > 0) {
            int logMaxTotal = PropertiesUtils.readInt(properties, "log_max_total", 28);
            int[] logPayloadByteSize = PropertiesUtils.readLogIntArray(properties, "log_payload_size");
            int[] elementByteLens = Arrays.stream(logPayloadByteSize).map(each -> 1 << each).toArray();
            for (int dataSize : dataSizes) {
                for (int elementByteLen : elementByteLens) {
                    if (LongUtils.ceilLog2(dataSize) + LongUtils.ceilLog2(elementByteLen) <= logMaxTotal) {
                        parameters.add(new int[]{dataSize, elementByteLen});
                    }
                }
            }
        }
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws MpcAbortException, IOException {
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = filePathString + File.separator + PTO_TYPE_NAME
            + "_" + rosnConfig.getPtoType().name()
            + "_" + appendString
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID,Data Size,Payload Byte Length,Parallel, "
            + "Init Time(ms),Init DataPacket Num,Init Payload Bytes(B),Init Send Bytes(B), "
            + "Pto  Time(ms),Pto  DataPacket Num,Pto  Payload Bytes(B),Pto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 建立连接
        serverRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, rosnConfig, taskId);
        taskId++;
        // 正式测试
        for (int[] param : parameters) {
            runServer(serverRpc, clientParty, rosnConfig, taskId, true, param[0], param[1], printWriter);
            taskId++;
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, RosnConfig config, int taskId) throws MpcAbortException {
        RosnSender rosnSender = RosnFactory.createSender(serverRpc, clientParty, config);
        rosnSender.setTaskId(taskId);
        rosnSender.setParallel(false);
        rosnSender.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", rosnSender.ownParty().getPartyName());
        rosnSender.init();
        rosnSender.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", rosnSender.ownParty().getPartyName());
        rosnSender.rosn(WARMUP_DATA_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        rosnSender.getRpc().synchronize();
        rosnSender.getRpc().reset();
        LOGGER.info("(warmup) {} finish", rosnSender.ownParty().getPartyName());
        System.gc();
    }

    public void runServer(Rpc serverRpc, Party clientParty, RosnConfig config, int taskId, boolean parallel,
                          int dataSize, int payloadByteLen,
                          PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "\n\n{}: dataSize = {}, payloadByteLen = {}, parallel = {} \n\n",
            serverRpc.ownParty().getPartyName(), dataSize, payloadByteLen, parallel
        );
        RosnSender rosnSender = RosnFactory.createSender(serverRpc, clientParty, config);
        rosnSender.setTaskId(taskId);
        rosnSender.setParallel(parallel);
        // 启动测试
        rosnSender.getRpc().synchronize();
        rosnSender.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", rosnSender.ownParty().getPartyName());
        stopWatch.start();
        rosnSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = rosnSender.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = rosnSender.getRpc().getPayloadByteLength();
        long initSendByteLength = rosnSender.getRpc().getSendByteLength();
        rosnSender.getRpc().synchronize();
        rosnSender.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", rosnSender.ownParty().getPartyName());
        stopWatch.start();
        rosnSender.rosn(dataSize, payloadByteLen);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = rosnSender.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = rosnSender.getRpc().getPayloadByteLength();
        long ptoSendByteLength = rosnSender.getRpc().getSendByteLength();
        // 写入统计结果
        String info = rosnSender.ownParty().getPartyId()
            + "," + dataSize + "," + payloadByteLen + "," + rosnSender.getParallel()
            + "," + initTime + "," + initDataPacketNum + "," + initPayloadByteLength + "," + initSendByteLength
            + "," + ptoTime + "," + ptoDataPacketNum + "," + ptoPayloadByteLength + "," + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        rosnSender.getRpc().synchronize();
        rosnSender.getRpc().reset();
        LOGGER.info("{} finish", rosnSender.ownParty().getPartyName());
        System.gc();
    }

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws IOException, MpcAbortException {
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = filePathString + File.separator + PTO_TYPE_NAME
            + "_" + rosnConfig.getPtoType().name()
            + "_" + appendString
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID,Data Size,Payload Byte Length,Parallel, "
            + "Init Time(ms),Init DataPacket Num,Init Payload Bytes(B),Init Send Bytes(B), "
            + "Pto  Time(ms),Pto  DataPacket Num,Pto  Payload Bytes(B),Pto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, rosnConfig, taskId);
        taskId++;
        for (int[] param : parameters) {
            runClient(clientRpc, serverParty, rosnConfig, taskId, true, param[0], param[1], printWriter);
            taskId++;
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, RosnConfig config, int taskId) throws MpcAbortException {
        RosnReceiver rosnReceiver = RosnFactory.createReceiver(clientRpc, serverParty, config);
        rosnReceiver.setTaskId(taskId);
        rosnReceiver.setParallel(false);
        int[] pi = PermutationNetworkUtils.randomPermutation(WARMUP_DATA_SIZE, new SecureRandom());
        rosnReceiver.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", rosnReceiver.ownParty().getPartyName());
        rosnReceiver.init();
        rosnReceiver.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", rosnReceiver.ownParty().getPartyName());
        rosnReceiver.rosn(pi, WARMUP_ELEMENT_BYTE_LENGTH);
        // 同步并等待5秒钟，保证对方执行完毕
        rosnReceiver.getRpc().synchronize();
        rosnReceiver.getRpc().reset();
        LOGGER.info("(warmup) {} finish", rosnReceiver.ownParty().getPartyName());
        System.gc();
    }

    public void runClient(Rpc clientRpc, Party serverParty, RosnConfig config, int taskId, boolean parallel,
                          int dataSize, int payloadByteLen,
                          PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "\n\n{}: dataSize = {}, payloadByteLen = {}, parallel = {}\n\n",
            clientRpc.ownParty().getPartyName(), dataSize, payloadByteLen, parallel
        );
        RosnReceiver rosnReceiver = RosnFactory.createReceiver(clientRpc, serverParty, config);
        rosnReceiver.setTaskId(taskId);
        rosnReceiver.setParallel(parallel);
        int[] pi = PermutationNetworkUtils.randomPermutation(dataSize, new SecureRandom());
        // 启动测试
        rosnReceiver.getRpc().synchronize();
        rosnReceiver.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", rosnReceiver.ownParty().getPartyName());
        stopWatch.start();
        rosnReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = rosnReceiver.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = rosnReceiver.getRpc().getPayloadByteLength();
        long initSendByteLength = rosnReceiver.getRpc().getSendByteLength();
        rosnReceiver.getRpc().synchronize();
        rosnReceiver.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", rosnReceiver.ownParty().getPartyName());
        stopWatch.start();
        rosnReceiver.rosn(pi, payloadByteLen);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = rosnReceiver.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = rosnReceiver.getRpc().getPayloadByteLength();
        long ptoSendByteLength = rosnReceiver.getRpc().getSendByteLength();
        // 写入统计结果
        String info = rosnReceiver.ownParty().getPartyId()
            + "," + dataSize + "," + payloadByteLen + "," + rosnReceiver.getParallel()
            + "," + initTime + "," + initDataPacketNum + "," + initPayloadByteLength + "," + initSendByteLength
            + "," + ptoTime + "," + ptoDataPacketNum + "," + ptoPayloadByteLength + "," + ptoSendByteLength;
        printWriter.println(info);
        rosnReceiver.getRpc().synchronize();
        rosnReceiver.getRpc().reset();
        LOGGER.info("{} finish", rosnReceiver.ownParty().getPartyName());
        System.gc();
    }
}

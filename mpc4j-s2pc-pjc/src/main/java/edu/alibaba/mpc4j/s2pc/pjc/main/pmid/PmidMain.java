package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PMID主函数。
 *
 * @author Weiran Liu
 * @date 2022/5/17
 */
public class PmidMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "PMID";
    /**
     * 预热元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热最大u
     */
    private static final int WARMUP_MAX_U = 1;
    /**
     * 预热集合大小
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * server stop watch
     */
    private final StopWatch serverStopWatch;
    /**
     * server stop watch
     */
    private final StopWatch clientStopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;

    public PmidMain(Properties properties) {
        this.properties = properties;
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
    }

    public void runNetty() throws Exception {
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    public void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logNonSideSetSizes = PropertiesUtils.readLogIntArray(properties, "non_side_log_set_size");
        int[] nonSideSetSizes = Arrays.stream(logNonSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] logOneSideSetSizes = PropertiesUtils.readLogIntArray(properties, "one_side_log_set_size");
        int[] oneSideSetSizes = Arrays.stream(logOneSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] logTwoSideSetSizes = PropertiesUtils.readLogIntArray(properties, "two_side_log_set_size");
        int[] twoSideSetSizes = Arrays.stream(logTwoSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取max(u)
        int maxU = PropertiesUtils.readInt(properties, "max_u");
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PmidConfig config = PmidConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSize : nonSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        for (int setSize : oneSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        for (int setSize : twoSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PsoUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tServerU\tClient Set Size\tClientU\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 建立连接
        serverRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        // 两边均为集合的测试
        for (int setSize : nonSideSetSizes) {
            Map<ByteBuffer, Integer> serverElementMap = getServerElementMap(setSize, 1);
            // 多线程
            runServer(serverRpc, clientParty, config, taskId, true, serverElementMap, 1, setSize, 1, printWriter);
            taskId++;
            // 单线程
            runServer(serverRpc, clientParty, config, taskId, false, serverElementMap, 1, setSize, 1, printWriter);
            taskId++;
        }
        // 服务端为集合的测试
        for (int setSize : oneSideSetSizes) {
            Map<ByteBuffer, Integer> serverElementMap = getServerElementMap(setSize, 1);
            // 多线程
            runServer(serverRpc, clientParty, config, taskId, true, serverElementMap, 1, setSize, maxU, printWriter);
            taskId++;
            // 单线程
            runServer(serverRpc, clientParty, config, taskId, false, serverElementMap, 1, setSize, maxU, printWriter);
            taskId++;
        }
        // 两边均为多集合的测试
        for (int setSize : twoSideSetSizes) {
            Map<ByteBuffer, Integer> serverElementMap = getServerElementMap(setSize, maxU);
            // 多线程
            runServer(serverRpc, clientParty, config, taskId, true, serverElementMap, maxU, setSize, maxU, printWriter);
            taskId++;
            // 单线程
            runServer(serverRpc, clientParty, config, taskId, false, serverElementMap, maxU, setSize, maxU, printWriter);
            taskId++;
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Map<ByteBuffer, Integer> getServerElementMap(int setSize, int serverU) throws IOException {
        LOGGER.info("Server read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Map<ByteBuffer, Integer> serverElementMap = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toMap(
                element -> element,
                element -> serverU
            ));
        bufferedReader.close();
        inputStreamReader.close();
        return serverElementMap;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PmidConfig config, int taskId) throws Exception {
        Map<ByteBuffer, Integer> serverElementMap = getServerElementMap(WARMUP_SET_SIZE, WARMUP_MAX_U);
        PmidServer<ByteBuffer> pmidServer = PmidFactory.createServer(serverRpc, clientParty, config);
        pmidServer.setTaskId(taskId);
        pmidServer.setParallel(true);
        pmidServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmidServer.ownParty().getPartyName());
        pmidServer.init(WARMUP_SET_SIZE, WARMUP_MAX_U, WARMUP_SET_SIZE, WARMUP_MAX_U);
        pmidServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmidServer.ownParty().getPartyName());
        pmidServer.pmid(serverElementMap, WARMUP_SET_SIZE, WARMUP_MAX_U);
        // 同步
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        pmidServer.destroy();
        LOGGER.info("(warmup) {} finish", pmidServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, PmidConfig config, int taskId, boolean parallel,
                           Map<ByteBuffer, Integer> serverElementMap, int serverU, int clientSetSize, int clientU,
                           PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementMap.keySet().size();
        LOGGER.info(
            "{}: serverSetSize = {}, serverU = {}, clientSetSize = {}, clientU = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, serverU, clientSetSize, clientU, parallel
        );
        PmidServer<ByteBuffer> pmidServer = PmidFactory.createServer(serverRpc, clientParty, config);
        pmidServer.setTaskId(taskId);
        pmidServer.setParallel(parallel);
        // 启动测试
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmidServer.ownParty().getPartyName());
        serverStopWatch.start();
        pmidServer.init(serverSetSize, serverU, clientSetSize, clientU);
        serverStopWatch.stop();
        long initTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long initDataPacketNum = pmidServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmidServer.getRpc().getPayloadByteLength();
        long initSendByteLength = pmidServer.getRpc().getSendByteLength();
        // 同步
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmidServer.ownParty().getPartyName());
        serverStopWatch.start();
        pmidServer.pmid(serverElementMap, clientSetSize, clientU);
        serverStopWatch.stop();
        long ptoTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long ptoDataPacketNum = pmidServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmidServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmidServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmidServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + serverU
            + "\t" + clientSetSize
            + "\t" + clientU
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        pmidServer.destroy();
        LOGGER.info("{} finish", pmidServer.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logNonSideSetSizes = PropertiesUtils.readLogIntArray(properties, "non_side_log_set_size");
        int[] nonSideSetSizes = Arrays.stream(logNonSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] logOneSideSetSizes = PropertiesUtils.readLogIntArray(properties, "one_side_log_set_size");
        int[] oneSideSetSizes = Arrays.stream(logOneSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] logTwoSideSetSizes = PropertiesUtils.readLogIntArray(properties, "two_side_log_set_size");
        int[] twoSideSetSizes = Arrays.stream(logTwoSideSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取max(k)
        int maxU = PropertiesUtils.readInt(properties, "max_u");
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PmidConfig config = PmidConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSize : nonSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        for (int setSize : oneSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        for (int setSize : twoSideSetSizes) {
            PsoUtils.generateBytesInputFiles(setSize, ELEMENT_BYTE_LENGTH);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PsoUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tServerU\tClient Set Size\tClientU\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        // 两边均为集合的测试
        for (int setSize : nonSideSetSizes) {
            Map<ByteBuffer, Integer> clientElementMap = getClientElementMap(setSize, 1);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementMap, 1, setSize, 1, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementMap, 1, setSize, 1, printWriter);
            taskId++;
        }
        // 服务端为集合的测试
        for (int setSize : oneSideSetSizes) {
            Map<ByteBuffer, Integer> clientElementMap = getClientElementMap(setSize, maxU);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementMap, maxU, setSize, 1, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementMap, maxU, setSize, 1, printWriter);
            taskId++;
        }
        // 两边均为多集合的测试
        for (int setSize : twoSideSetSizes) {
            Map<ByteBuffer, Integer> clientElementMap = getClientElementMap(setSize, maxU);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementMap, maxU, setSize, maxU, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementMap, maxU, setSize, maxU, printWriter);
            taskId++;
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Map<ByteBuffer, Integer> getClientElementMap(int setSize, int clientU) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Map<ByteBuffer, Integer> clientElementMap = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toMap(
                element -> element,
                element -> clientU
            ));
        bufferedReader.close();
        inputStreamReader.close();
        return clientElementMap;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PmidConfig config, int taskId) throws Exception {
        Map<ByteBuffer, Integer> clientElementMap = getClientElementMap(WARMUP_SET_SIZE, WARMUP_MAX_U);
        PmidClient<ByteBuffer> pmidClient = PmidFactory.createClient(clientRpc, serverParty, config);
        pmidClient.setTaskId(taskId);
        pmidClient.setParallel(true);
        pmidClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmidClient.ownParty().getPartyName());
        pmidClient.init(WARMUP_SET_SIZE, WARMUP_MAX_U, WARMUP_SET_SIZE, WARMUP_MAX_U);
        pmidClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmidClient.ownParty().getPartyName());
        pmidClient.pmid(clientElementMap, WARMUP_SET_SIZE);
        pmidClient.getRpc().getSendDataPacketNum();
        pmidClient.getRpc().getPayloadByteLength();
        pmidClient.getRpc().getSendByteLength();
        // 同步
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        pmidClient.destroy();
        LOGGER.info("(warmup) {} finish", pmidClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, PmidConfig config, int taskId, boolean parallel,
                           Map<ByteBuffer, Integer> clientElementMap, int clientU, int serverSetSize, int serverU,
                           PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementMap.keySet().size();
        LOGGER.info(
            "{}: serverSetSize = {}, serverU = {}, clientSetSize = {}, clientU = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, serverU, clientSetSize, clientU, parallel
        );
        PmidClient<ByteBuffer> pmidClient = PmidFactory.createClient(clientRpc, serverParty, config);
        pmidClient.setTaskId(taskId);
        pmidClient.setParallel(parallel);
        // 启动测试
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmidClient.ownParty().getPartyName());
        clientStopWatch.start();
        pmidClient.init(clientSetSize, clientU, serverSetSize, serverU);
        clientStopWatch.stop();
        long initTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long initDataPacketNum = pmidClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmidClient.getRpc().getPayloadByteLength();
        long initSendByteLength = pmidClient.getRpc().getSendByteLength();
        // 同步
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmidClient.ownParty().getPartyName());
        clientStopWatch.start();
        pmidClient.pmid(clientElementMap, serverSetSize, serverU);
        clientStopWatch.stop();
        long ptoTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long ptoDataPacketNum = pmidClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmidClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmidClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmidClient.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + serverU
            + "\t" + clientSetSize
            + "\t" + clientU
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        pmidClient.destroy();
        LOGGER.info("{} finish", pmidClient.ownParty().getPartyName());
    }
}

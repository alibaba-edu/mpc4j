package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PID主函数。
 *
 * @author Weiran Liu
 * @date 2022/5/16
 */
public class PidMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "PID";
    /**
     * 预热元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热
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

    public PidMain(Properties properties) {
        this.properties = properties;
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
    }

    public void runNetty() throws Exception {
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else {
            runClient(ownRpc, ownRpc.getParty(0));
        }
    }

    public void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PidConfig config = PidConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
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
        String tab = "Party ID\tServer Element Size\tClient Element Size\tIs Parallel\tThread Num"
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
        // 正式测试
        for (int setSize : setSizes) {
            // 读取输入文件
            Set<ByteBuffer> serverElementSet = readServerElementSet(setSize);
            // 多线程
            runServer(serverRpc, clientParty, config, taskId, true, serverElementSet, setSize, printWriter);
            taskId++;
            // 单线程
            runServer(serverRpc, clientParty, config, taskId, false, serverElementSet, setSize, printWriter);
            taskId++;
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readServerElementSet(int setSize) throws IOException {
        // 读取输入文件
        LOGGER.info("Server read element set, size = " + setSize);
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return serverElementSet;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PidConfig config, int taskId) throws Exception {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE);
        PidParty<ByteBuffer> pidServer = PidFactory.createServer(serverRpc, clientParty, config);
        pidServer.setTaskId(taskId);
        pidServer.setParallel(true);
        pidServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pidServer.ownParty().getPartyName());
        pidServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        pidServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pidServer.ownParty().getPartyName());
        pidServer.pid(serverElementSet, WARMUP_SET_SIZE);
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        pidServer.destroy();
        LOGGER.info("(warmup) {} finish", pidServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, PidConfig config, int taskId, boolean parallel,
                           Set<ByteBuffer> serverElementSet, int clientSetSize,
                           PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PidParty<ByteBuffer> pidServer = PidFactory.createServer(serverRpc, clientParty, config);
        pidServer.setTaskId(taskId);
        pidServer.setParallel(parallel);
        // 启动测试
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pidServer.ownParty().getPartyName());
        serverStopWatch.start();
        pidServer.init(serverSetSize, clientSetSize);
        serverStopWatch.stop();
        long initTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long initDataPacketNum = pidServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pidServer.getRpc().getPayloadByteLength();
        long initSendByteLength = pidServer.getRpc().getSendByteLength();
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pidServer.ownParty().getPartyName());
        serverStopWatch.start();
        pidServer.pid(serverElementSet, clientSetSize);
        serverStopWatch.stop();
        long ptoTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long ptoDataPacketNum = pidServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pidServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pidServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pidServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + pidServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        pidServer.destroy();
        LOGGER.info("{} finish", pidServer.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int[] setSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PidConfig config = PidConfigUtils.createConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
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
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
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
        for (int setSize : setSizes) {
            Set<ByteBuffer> clientElementSet = readClientElementSet(setSize);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementSet, setSize, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementSet, setSize, printWriter);
            taskId++;
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, ELEMENT_BYTE_LENGTH)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return clientElementSet;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PidConfig config, int taskId) throws Exception {
        // 读取输入文件
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE);
        PidParty<ByteBuffer> pidClient = PidFactory.createClient(clientRpc, serverParty, config);
        pidClient.setTaskId(taskId);
        pidClient.setParallel(true);
        pidClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pidClient.ownParty().getPartyName());
        pidClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        pidClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pidClient.ownParty().getPartyName());
        pidClient.pid(clientElementSet, WARMUP_SET_SIZE);
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        pidClient.destroy();
        LOGGER.info("(warmup) {} finish", pidClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, PidConfig config, int taskId, boolean parallel,
                           Set<ByteBuffer> clientElementSet, int serverSetSize,
                           PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PidParty<ByteBuffer> pidClient = PidFactory.createClient(clientRpc, serverParty, config);
        pidClient.setTaskId(taskId);
        pidClient.setParallel(parallel);
        // 启动测试
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pidClient.ownParty().getPartyName());
        clientStopWatch.start();
        pidClient.init(clientSetSize, serverSetSize);
        clientStopWatch.stop();
        long initTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long initDataPacketNum = pidClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pidClient.getRpc().getPayloadByteLength();
        long initSendByteLength = pidClient.getRpc().getSendByteLength();
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pidClient.ownParty().getPartyName());
        clientStopWatch.start();
        pidClient.pid(clientElementSet, serverSetSize);
        clientStopWatch.stop();
        long ptoTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long ptoDataPacketNum = pidClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pidClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pidClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pidClient.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + pidClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        pidClient.destroy();
        LOGGER.info("{} finish", pidClient.ownParty().getPartyName());
    }
}

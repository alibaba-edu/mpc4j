package edu.alibaba.mpc4j.s2pc.pso.main.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMainUtils;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidParty;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
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
    private static final String PTO_TYPE_NAME = "PID";
    /**
     * 预热元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * 配置参数
     */
    private final Properties properties;

    public PidMain(Properties properties) {
        this.properties = properties;
    }

    public void run() throws Exception {
        Rpc ownRpc = PsoMainUtils.setRpc(properties);
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] setSizes = PsoMainUtils.readSetSizes(properties);
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
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
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
        // 正式测试
        for (int setSize : setSizes) {
            // 读取输入文件
            LOGGER.info("{} read element set", serverRpc.ownParty().getPartyName());
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
            // 多线程
            LOGGER.info("{} multiple thread test", serverRpc.ownParty().getPartyName());
            taskId++;
            PidParty<ByteBuffer> parallelPidServer = PidFactory.createServer(serverRpc, clientParty, config);
            parallelPidServer.setTaskId(taskId);
            parallelPidServer.setParallel(true);
            runServer(parallelPidServer, serverElementSet, setSize, printWriter);
            // 单线程
            LOGGER.info("{} single thread test", serverRpc.ownParty().getPartyName());
            taskId++;
            PidParty<ByteBuffer> sequencePidServer = PidFactory.createServer(serverRpc, clientParty, config);
            sequencePidServer.setTaskId(taskId);
            sequencePidServer.setParallel(false);
            runServer(sequencePidServer, serverElementSet, setSize, printWriter);
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PidConfig config, int taskId) throws Exception {
        LOGGER.info("(warmup) {} read element set", serverRpc.ownParty().getPartyName());
        InputStreamReader warmupInputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX,
                WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH)), CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(warmupInputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        warmupInputStreamReader.close();
        PidParty<ByteBuffer> pidServer = PidFactory.createServer(serverRpc, clientParty, config);
        pidServer.setTaskId(taskId);
        pidServer.setParallel(false);
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
        LOGGER.info("(warmup) {} finish", pidServer.ownParty().getPartyName());
    }

    private void runServer(PidParty<ByteBuffer> pidServer, Set<ByteBuffer> serverElementSet, int clientElementSize,
                           PrintWriter printWriter) throws Exception {
        int serverElementSize = serverElementSet.size();
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pidServer.ownParty().getPartyName());
        stopWatch.start();
        pidServer.init(serverElementSize, clientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = pidServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pidServer.getRpc().getPayloadByteLength();
        long initSendByteLength = pidServer.getRpc().getSendByteLength();
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pidServer.ownParty().getPartyName());
        stopWatch.start();
        pidServer.pid(serverElementSet, clientElementSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = pidServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pidServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pidServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pidServer.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + clientElementSize
            + "\t" + pidServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        pidServer.getRpc().synchronize();
        pidServer.getRpc().reset();
        LOGGER.info("{} finish", pidServer.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] setSizes = PsoMainUtils.readSetSizes(properties);
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
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + ELEMENT_BYTE_LENGTH * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tClient Element Size\tServer Element Size\tIs Parallel\tThread Num"
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
        for (int setSize : setSizes) {
            // 读取输入文件
            LOGGER.info("{} read element set", clientRpc.ownParty().getPartyName());
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
            // 多线程
            LOGGER.info("{} multiple thread test", clientRpc.ownParty().getPartyName());
            taskId++;
            PidParty<ByteBuffer> parallelPidClient = PidFactory.createClient(clientRpc, serverParty, config);
            parallelPidClient.setTaskId(taskId);
            parallelPidClient.setParallel(true);
            runClient(parallelPidClient, clientElementSet, setSize, printWriter);
            // 单线程
            LOGGER.info("{} single thread test", clientRpc.ownParty().getPartyName());
            taskId++;
            PidParty<ByteBuffer> sequencePidClient = PidFactory.createClient(clientRpc, serverParty, config);
            sequencePidClient.setTaskId(taskId);
            sequencePidClient.setParallel(false);
            runClient(sequencePidClient, clientElementSet, setSize, printWriter);
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PidConfig config, int taskId) throws Exception {
        // 读取输入文件
        LOGGER.info("(warmup) {} read element set", clientRpc.ownParty().getPartyName());
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX,
                WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH)), CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        PidParty<ByteBuffer> pidClient = PidFactory.createClient(clientRpc, serverParty, config);
        pidClient.setTaskId(taskId);
        pidClient.setParallel(false);
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
        LOGGER.info("(warmup) {} finish", pidClient.ownParty().getPartyName());
    }

    private void runClient(PidParty<ByteBuffer> pidClient, Set<ByteBuffer> clientElementSet, int serverElementSize,
                           PrintWriter printWriter) throws Exception {
        int clientElementSize = clientElementSet.size();
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pidClient.ownParty().getPartyName());
        stopWatch.start();
        pidClient.init(clientElementSize, serverElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = pidClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pidClient.getRpc().getPayloadByteLength();
        long initSendByteLength = pidClient.getRpc().getSendByteLength();
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pidClient.ownParty().getPartyName());
        stopWatch.start();
        pidClient.pid(clientElementSet, serverElementSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = pidClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pidClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pidClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pidClient.ownParty().getPartyId()
            + "\t" + clientElementSize
            + "\t" + serverElementSize
            + "\t" + pidClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步并等待5秒钟，保证对方执行完毕
        pidClient.getRpc().synchronize();
        pidClient.getRpc().reset();
        LOGGER.info("{} finish", pidClient.ownParty().getPartyName());
    }
}

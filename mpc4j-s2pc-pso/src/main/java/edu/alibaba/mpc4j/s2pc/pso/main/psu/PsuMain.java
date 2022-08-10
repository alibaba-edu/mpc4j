package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMainUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
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
 * PSU主函数。
 *
 * @author Weiran Liu
 * @date 2021/09/14
 */
public class PsuMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * 协议类型名称
     */
    private static final String PTO_TYPE_NAME = "PSU";
    /**
     * 预热元素字节长度
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * 配置参数
     */
    private final Properties properties;

    public PsuMain(Properties properties) {
        this.properties = properties;
    }

    public void run() throws Exception {
        Rpc ownRpc = PsoMainUtils.setRpc(properties);
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1){
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取元素字节长度
        int elementByteLength = PsoMainUtils.readElementByteLength(properties);
        // 读取集合大小
        int[] setSizes = PsoMainUtils.readSetSizes(properties);
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
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
                new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength)),
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
            PsuServer parallelPsuServer = PsuFactory.createServer(serverRpc, clientParty, config);
            parallelPsuServer.setTaskId(taskId);
            parallelPsuServer.setParallel(true);
            runServer(parallelPsuServer, serverElementSet, setSize, elementByteLength, printWriter);
            // 单线程
            LOGGER.info("{} single thread test", serverRpc.ownParty().getPartyName());
            taskId++;
            PsuServer sequencePsuServer = PsuFactory.createServer(serverRpc, clientParty, config);
            sequencePsuServer.setTaskId(taskId);
            sequencePsuServer.setParallel(false);
            runServer(sequencePsuServer, serverElementSet, setSize, elementByteLength, printWriter);
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PsuConfig config, int taskId) throws Exception {
        LOGGER.info("(warmup) {} read element set", serverRpc.ownParty().getPartyName());
        InputStreamReader warmupInputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX,
                WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH)), CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(warmupInputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        warmupInputStreamReader.close();
        PsuServer psuServer = PsuFactory.createServer(serverRpc, clientParty, config);
        psuServer.setTaskId(taskId);
        psuServer.setParallel(false);
        psuServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", psuServer.ownParty().getPartyName());
        psuServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        psuServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", psuServer.ownParty().getPartyName());
        psuServer.psu(serverElementSet, WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        LOGGER.info("(warmup) {} finish", psuServer.ownParty().getPartyName());
    }

    private void runServer(PsuServer psuServer,
                           Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength,
                           PrintWriter printWriter) throws Exception {
        int serverElementSize = serverElementSet.size();
        LOGGER.info("----- {} start, ServerSetSize = {}, ClientSetSize = {}, parallel = {}",
            psuServer.getPtoDesc().getPtoName(), serverElementSize, clientElementSize, psuServer.getParallel()
        );
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuServer.ownParty().getPartyName());
        stopWatch.start();
        psuServer.init(serverElementSize, clientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long initSendByteLength = psuServer.getRpc().getSendByteLength();
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuServer.ownParty().getPartyName());
        stopWatch.start();
        psuServer.psu(serverElementSet, clientElementSize, elementByteLength);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuServer.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + clientElementSize
            + "\t" + psuServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        LOGGER.info("{} finish", psuServer.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取元素字节长度
        int elementByteLength = PsoMainUtils.readElementByteLength(properties);
        // 读取集合大小
        int[] setSizes = PsoMainUtils.readSetSizes(properties);
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSize : setSizes) {
            PsoUtils.generateBytesInputFiles(setSize, elementByteLength);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath =  PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
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
                new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength)),
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
            PsuClient parallelPsuClient = PsuFactory.createClient(clientRpc, serverParty, config);
            parallelPsuClient.setTaskId(taskId);
            parallelPsuClient.setParallel(true);
            runClient(parallelPsuClient, clientElementSet, setSize, elementByteLength, printWriter);
            // 单线程
            LOGGER.info("{} single thread test", clientRpc.ownParty().getPartyName());
            taskId++;
            PsuClient sequencePsuClient = PsuFactory.createClient(clientRpc, serverParty, config);
            sequencePsuClient.setTaskId(taskId);
            sequencePsuClient.setParallel(false);
            runClient(sequencePsuClient, clientElementSet, setSize, elementByteLength, printWriter);
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PsuConfig config, int taskId) throws Exception {
        // 读取输入文件
        LOGGER.info("(warmup) {} read element set", clientRpc.ownParty().getPartyName());
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX,
                WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH)), CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        PsuClient psuClient = PsuFactory.createClient(clientRpc, serverParty, config);
        psuClient.setTaskId(taskId);
        psuClient.setParallel(false);
        psuClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", psuClient.ownParty().getPartyName());
        psuClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        psuClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", psuClient.ownParty().getPartyName());
        psuClient.psu(clientElementSet, WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        // 同步并等待5秒钟，保证对方执行完毕
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        LOGGER.info("(warmup) {} finish", psuClient.ownParty().getPartyName());
    }

    private void runClient(PsuClient psuClient,
                           Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength,
                           PrintWriter printWriter) throws Exception {
        int clientElementSize = clientElementSet.size();
        LOGGER.info("----- {} start, ClientSetSize = {}, ServerSetSize = {}, parallel = {}",
            psuClient.getPtoDesc().getPtoName(), clientElementSize, serverElementSize, psuClient.getParallel()
        );
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuClient.ownParty().getPartyName());
        stopWatch.start();
        psuClient.init(clientElementSize, serverElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long initSendByteLength = psuClient.getRpc().getSendByteLength();
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuClient.ownParty().getPartyName());
        stopWatch.start();
        psuClient.psu(clientElementSet, serverElementSize, elementByteLength);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuClient.ownParty().getPartyId()
            + "\t" + clientElementSize
            + "\t" + serverElementSize
            + "\t" + psuClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        LOGGER.info("{} finish", psuClient.ownParty().getPartyName());
    }
}

package edu.alibaba.mpc4j.s2pc.pso.main.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMainUtils;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidClient;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidFactory;
import edu.alibaba.mpc4j.s2pc.pso.pmid.PmidServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
    private static final String PTO_TYPE_NAME = "PMID";
    /**
     * 预热元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = 16;
    /**
     * 默认最大k
     */
    private static final int DEFAULT_MAX_K = 1;
    /**
     * 预热
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * 配置参数
     */
    private final Properties properties;

    public PmidMain(Properties properties) {
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
        // 读取max(k)
        int[] maxKs = PsoMainUtils.readMaxKs(properties);
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PmidConfig config = PmidConfigUtils.createConfig(properties);
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
        String tab = "Party ID\tmax(k)\tServer Element Size\tClient Element Size\tIs Parallel\tThread Num"
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
        for (int maxK : maxKs) {
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
                PmidServer<ByteBuffer> parallelPmidServer = PmidFactory.createServer(serverRpc, clientParty, config);
                parallelPmidServer.setTaskId(taskId);
                parallelPmidServer.setParallel(true);
                runServer(parallelPmidServer, serverElementSet, setSize, maxK, printWriter);
                // 单线程
                LOGGER.info("{} single thread test", serverRpc.ownParty().getPartyName());
                taskId++;
                PmidServer<ByteBuffer> sequencePmidServer = PmidFactory.createServer(serverRpc, clientParty, config);
                sequencePmidServer.setTaskId(taskId);
                sequencePmidServer.setParallel(false);
                runServer(sequencePmidServer, serverElementSet, setSize, maxK, printWriter);
            }
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PmidConfig config, int taskId) throws Exception {
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
        PmidServer<ByteBuffer> pmidServer = PmidFactory.createServer(serverRpc, clientParty, config);
        pmidServer.setTaskId(taskId);
        pmidServer.setParallel(false);
        pmidServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmidServer.ownParty().getPartyName());
        pmidServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE, DEFAULT_MAX_K);
        pmidServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmidServer.ownParty().getPartyName());
        pmidServer.pmid(serverElementSet, WARMUP_SET_SIZE, DEFAULT_MAX_K);
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        LOGGER.info("(warmup) {} finish", pmidServer.ownParty().getPartyName());
    }

    private void runServer(PmidServer<ByteBuffer> pmidServer, Set<ByteBuffer> serverElementSet,
                           int clientElementSize, int maxK,
                           PrintWriter printWriter) throws Exception {
        int serverElementSize = serverElementSet.size();
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        // 同步并等待5秒钟，保证对方启动
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmidServer.ownParty().getPartyName());
        stopWatch.start();
        pmidServer.init(serverElementSize, clientElementSize, maxK);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = pmidServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmidServer.getRpc().getPayloadByteLength();
        long initSendByteLength = pmidServer.getRpc().getSendByteLength();
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmidServer.ownParty().getPartyName());
        stopWatch.start();
        pmidServer.pmid(serverElementSet, clientElementSize, maxK);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = pmidServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmidServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmidServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmidServer.ownParty().getPartyId()
            + "\t" + maxK
            + "\t" + serverElementSize
            + "\t" + clientElementSize
            + "\t" + pmidServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步并等待5秒钟，保证对方执行完毕
        pmidServer.getRpc().synchronize();
        pmidServer.getRpc().reset();
        LOGGER.info("{} finish", pmidServer.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取集合大小
        int[] setSizes = PsoMainUtils.readSetSizes(properties);
        // 读取max(k)
        int[] maxKs = PsoMainUtils.readMaxKs(properties);
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PmidConfig config = PmidConfigUtils.createConfig(properties);
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
        String tab = "Party ID\tmax(k)\tClient Element Size\tServer Element Size\tIs Parallel\tThread Num"
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
        for (int maxK : maxKs) {
            for (int setSize : setSizes) {
                // 读取输入文件
                LOGGER.info("{} read element set", clientRpc.ownParty().getPartyName());
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
                        element -> maxK
                    ));
                bufferedReader.close();
                inputStreamReader.close();
                // 多线程
                LOGGER.info("{} multiple thread test", clientRpc.ownParty().getPartyName());
                taskId++;
                PmidClient<ByteBuffer> parallelPmidClient = PmidFactory.createClient(clientRpc, serverParty, config);
                parallelPmidClient.setTaskId(taskId);
                parallelPmidClient.setParallel(true);
                runClient(parallelPmidClient, clientElementMap, setSize, maxK, printWriter);
                // 单线程
                LOGGER.info("{} single thread test", clientRpc.ownParty().getPartyName());
                taskId++;
                PmidClient<ByteBuffer> sequencePmidClient = PmidFactory.createClient(clientRpc, serverParty, config);
                sequencePmidClient.setTaskId(taskId);
                sequencePmidClient.setParallel(false);
                runClient(sequencePmidClient, clientElementMap, setSize, maxK, printWriter);
            }
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PmidConfig config, int taskId) throws Exception {
        // 读取输入文件
        LOGGER.info("(warmup) {} read element set", clientRpc.ownParty().getPartyName());
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX,
                WARMUP_SET_SIZE, ELEMENT_BYTE_LENGTH)), CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Map<ByteBuffer, Integer> clientElementMap = bufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toMap(
                element -> element,
                element -> DEFAULT_MAX_K
            ));
        bufferedReader.close();
        inputStreamReader.close();
        PmidClient<ByteBuffer> pmidClient = PmidFactory.createClient(clientRpc, serverParty, config);
        pmidClient.setTaskId(taskId);
        pmidClient.setParallel(false);
        pmidClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", pmidClient.ownParty().getPartyName());
        pmidClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE, DEFAULT_MAX_K);
        pmidClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", pmidClient.ownParty().getPartyName());
        pmidClient.pmid(clientElementMap, WARMUP_SET_SIZE);
        pmidClient.getRpc().getSendDataPacketNum();
        pmidClient.getRpc().getPayloadByteLength();
        pmidClient.getRpc().getSendByteLength();
        // 同步并等待5秒钟，保证对方执行完毕
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        LOGGER.info("(warmup) {} finish", pmidClient.ownParty().getPartyName());
    }

    private void runClient(PmidClient<ByteBuffer> pmidClient, Map<ByteBuffer, Integer> clientElementMap,
                           int serverElementSize, int maxK,
                           PrintWriter printWriter) throws Exception {
        int clientElementSize = clientElementMap.keySet().size();
        // 启动测试
        StopWatch stopWatch = new StopWatch();
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", pmidClient.ownParty().getPartyName());
        stopWatch.start();
        pmidClient.init(clientElementSize, serverElementSize, maxK);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = pmidClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = pmidClient.getRpc().getPayloadByteLength();
        long initSendByteLength = pmidClient.getRpc().getSendByteLength();
        // 同步并等待5秒钟，保证对方初始化完毕
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", pmidClient.ownParty().getPartyName());
        stopWatch.start();
        pmidClient.pmid(clientElementMap, serverElementSize);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = pmidClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = pmidClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = pmidClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = pmidClient.ownParty().getPartyId()
            + "\t" + maxK
            + "\t" + clientElementSize
            + "\t" + serverElementSize
            + "\t" + pmidClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步并等待5秒钟，保证对方执行完毕
        pmidClient.getRpc().synchronize();
        pmidClient.getRpc().reset();
        LOGGER.info("{} finish", pmidClient.ownParty().getPartyName());
    }
}

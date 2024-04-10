package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
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
import java.util.Arrays;
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
    public static final String PTO_TYPE_NAME = "PSU";
    /**
     * 预热元素字节长度
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
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

    public PsuMain(Properties properties) {
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
        // 读取元素字节长度
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        // 读取集合大小
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PsoUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
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
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            runServer(serverRpc, clientParty, config, taskId, true, serverElementSet, clientSetSize,
                elementByteLength, printWriter);
            taskId++;
            runServer(serverRpc, clientParty, config, taskId, false, serverElementSet, clientSetSize,
                elementByteLength, printWriter);
            taskId++;
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readServerElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Server read element set");
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
        return serverElementSet;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, PsuConfig config, int taskId) throws Exception {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
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
        psuServer.destroy();
        LOGGER.info("(warmup) {} finish", psuServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, PsuConfig config, int taskId, boolean parallel,
                           Set<ByteBuffer> serverElementSet, int clientSetSize, int elementByteLength,
                           PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PsuServer psuServer = PsuFactory.createServer(serverRpc, clientParty, config);
        psuServer.setTaskId(taskId);
        psuServer.setParallel(parallel);
        // 启动测试
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuServer.ownParty().getPartyName());
        serverStopWatch.start();
        psuServer.init(serverSetSize, clientSetSize);
        serverStopWatch.stop();
        long initTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long initDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long initSendByteLength = psuServer.getRpc().getSendByteLength();
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuServer.ownParty().getPartyName());
        serverStopWatch.start();
        psuServer.psu(serverElementSet, clientSetSize, elementByteLength);
        serverStopWatch.stop();
        long ptoTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long ptoDataPacketNum = psuServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + psuServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        psuServer.getRpc().synchronize();
        psuServer.getRpc().reset();
        psuServer.destroy();
        LOGGER.info("{} finish", psuServer.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取元素字节长度
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        // 读取集合大小
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PsuConfig config = PsuConfigUtils.createPsuConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PsoUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
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
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            // 读取输入文件
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementSet, serverSetSize,
                elementByteLength, printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementSet, serverSetSize,
                elementByteLength, printWriter);
            taskId++;
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Client read element set");
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
        return clientElementSet;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, PsuConfig config, int taskId) throws Exception {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
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
        psuClient.destroy();
        LOGGER.info("(warmup) {} finish", psuClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, PsuConfig config, int taskId, boolean parallel,
                           Set<ByteBuffer> clientElementSet, int serverSetSize, int elementByteLength,
                           PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        PsuClient psuClient = PsuFactory.createClient(clientRpc, serverParty, config);
        psuClient.setTaskId(taskId);
        psuClient.setParallel(parallel);
        // 启动测试
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", psuClient.ownParty().getPartyName());
        clientStopWatch.start();
        psuClient.init(clientSetSize, serverSetSize);
        clientStopWatch.stop();
        long initTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long initDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long initSendByteLength = psuClient.getRpc().getSendByteLength();
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", psuClient.ownParty().getPartyName());
        clientStopWatch.start();
        psuClient.psu(clientElementSet, serverSetSize, elementByteLength);
        clientStopWatch.stop();
        long ptoTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long ptoDataPacketNum = psuClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = psuClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = psuClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = psuClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + psuClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        psuClient.getRpc().synchronize();
        psuClient.getRpc().reset();
        psuClient.destroy();
        LOGGER.info("{} finish", psuClient.ownParty().getPartyName());
    }
}

package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuServer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class PsuMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuMain.class);
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "psu_pto_name";
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
     * element byte length
     */
    private final int elementByteLength;
    /**
     * number of set sizes
     */
    private final int setSizeNum;
    /**
     * server set sizes
     */
    private final int[] serverSetSizes;
    /**
     * client set sizes
     */
    private final int[] clientSetSizes;
    /**
     * parallel
     */
    private final boolean parallel;
    /**
     * PSU config
     */
    private final PsuConfig psuConfig;

    public PsuMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read common config
        LOGGER.info("{} read common config", ownRpc.ownParty().getPartyName());
        elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
            serverLogSetSizes.length == clientLogSetSizes.length,
            "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
            serverLogSetSizes.length, clientLogSetSizes.length
        );
        setSizeNum = serverLogSetSizes.length;
        serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        parallel = PropertiesUtils.readBoolean(properties, "parallel", false);
        // read PSU config
        LOGGER.info("{} read PSU config", ownRpc.ownParty().getPartyName());
        psuConfig = PsuConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = MainPtoConfigUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + psuConfig.getPtoType().name()
            + "_" + appendString
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
        warmupServer(serverRpc, clientParty, psuConfig, taskId);
        System.gc();
        taskId++;
        // 正式测试
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            runServer(serverRpc, clientParty, psuConfig, taskId, serverElementSet, clientSetSize, elementByteLength, printWriter);
            System.gc();
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
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength))),
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

    private void warmupServer(Rpc serverRpc, Party clientParty, PsuConfig config, int taskId) throws IOException, MpcAbortException {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        PsuServer psuServer = PsuFactory.createServer(serverRpc, clientParty, config);
        psuServer.setTaskId(taskId);
        psuServer.setParallel(parallel);
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

    private void runServer(Rpc serverRpc, Party clientParty, PsuConfig config, int taskId,
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
        stopWatch.start();
        psuServer.init(serverSetSize, clientSetSize);
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
        psuServer.psu(serverElementSet, clientSetSize, elementByteLength);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
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

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws IOException, MpcAbortException {
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + PTO_TYPE_NAME
            + "_" + psuConfig.getPtoType().name()
            + "_" + appendString
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
        warmupClient(clientRpc, serverParty, psuConfig, taskId);
        System.gc();
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            // 读取输入文件
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            runClient(clientRpc, serverParty, psuConfig, taskId, clientElementSet, serverSetSize, elementByteLength, printWriter);
            System.gc();
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
            Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength))),
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

    private void warmupClient(Rpc clientRpc, Party serverParty, PsuConfig config, int taskId) throws IOException, MpcAbortException {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        PsuClient psuClient = PsuFactory.createClient(clientRpc, serverParty, config);
        psuClient.setTaskId(taskId);
        psuClient.setParallel(parallel);
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

    private void runClient(Rpc clientRpc, Party serverParty, PsuConfig config, int taskId,
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
        stopWatch.start();
        psuClient.init(clientSetSize, serverSetSize);
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
        psuClient.psu(clientElementSet, serverSetSize, elementByteLength);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
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

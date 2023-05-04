package edu.alibaba.mpc4j.s2pc.main.ucpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiServer;
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
 * UCPSI main.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class UcpsiMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiMain.class);
    /**
     * 任务名称
     */
    public static final String TASK_NAME = "UCPSI_TASK";
    /**
     * 预热元素字节长度
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * 预热
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * 预热
     */
    private static final int WARMUP_CLIENT_SET_SIZE = 1 << 5;
    /**
     * 秒表
     */
    private final StopWatch stopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;

    public UcpsiMain(Properties properties) {
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
        String ucpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        UcpsiType ucpsiType = UcpsiType.valueOf(ucpsiTypeString);
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
        UcpsiConfig config = UcpsiConfigUtils.createUcpsiConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = ucpsiType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num\tSilent"
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
            runServer(serverRpc, clientParty, config, taskId, serverElementSet, clientSetSize, printWriter);
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

    private void warmupServer(Rpc serverRpc, Party clientParty, UcpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        UcpsiServer ucpsiServer = UcpsiFactory.createServer(serverRpc, clientParty, config);
        ucpsiServer.setTaskId(taskId);
        ucpsiServer.setParallel(false);
        ucpsiServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", ucpsiServer.ownParty().getPartyName());
        ucpsiServer.init(serverElementSet, WARMUP_CLIENT_SET_SIZE);
        ucpsiServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", ucpsiServer.ownParty().getPartyName());
        ucpsiServer.psi();
        ucpsiServer.getRpc().synchronize();
        ucpsiServer.getRpc().reset();
        ucpsiServer.destroy();
        LOGGER.info("(warmup) {} finish", ucpsiServer.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, UcpsiConfig config, int taskId,
                           Set<ByteBuffer> serverElementSet, int clientSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        int serverSetSize = serverElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        UcpsiServer ucpsiServer = UcpsiFactory.createServer(serverRpc, clientParty, config);
        ucpsiServer.setTaskId(taskId);
        ucpsiServer.setParallel(true);
        // 启动测试
        ucpsiServer.getRpc().synchronize();
        ucpsiServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", ucpsiServer.ownParty().getPartyName());
        stopWatch.start();
        ucpsiServer.init(serverElementSet, clientSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = ucpsiServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = ucpsiServer.getRpc().getPayloadByteLength();
        long initSendByteLength = ucpsiServer.getRpc().getSendByteLength();
        ucpsiServer.getRpc().synchronize();
        ucpsiServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", ucpsiServer.ownParty().getPartyName());
        stopWatch.start();
        ucpsiServer.psi();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = ucpsiServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = ucpsiServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = ucpsiServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = ucpsiServer.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + clientSetSize
            + "\t" + ucpsiServer.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        ucpsiServer.getRpc().synchronize();
        ucpsiServer.getRpc().reset();
        ucpsiServer.destroy();
        LOGGER.info("{} finish", ucpsiServer.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        String ucpsiTypeString = PropertiesUtils.readString(properties, "pto_name");
        UcpsiType ucpsiType = UcpsiType.valueOf(ucpsiTypeString);
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
        UcpsiConfig config = UcpsiConfigUtils.createUcpsiConfig(properties);
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = ucpsiType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementByteLength * Byte.SIZE
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num\tSilent"
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
            runClient(clientRpc, serverParty, config, taskId, clientElementSet, serverSetSize, printWriter);
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

    private void warmupClient(Rpc clientRpc, Party serverParty, UcpsiConfig config, int taskId) throws Exception {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_CLIENT_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        UcpsiClient ucpsiClient = UcpsiFactory.createClient(clientRpc, serverParty, config);
        ucpsiClient.setTaskId(taskId);
        ucpsiClient.setParallel(false);
        ucpsiClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", ucpsiClient.ownParty().getPartyName());
        ucpsiClient.init(WARMUP_CLIENT_SET_SIZE, WARMUP_SERVER_SET_SIZE);
        ucpsiClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", ucpsiClient.ownParty().getPartyName());
        ucpsiClient.psi(clientElementSet);
        // 同步并等待5秒钟，保证对方执行完毕
        ucpsiClient.getRpc().synchronize();
        ucpsiClient.getRpc().reset();
        ucpsiClient.destroy();
        LOGGER.info("(warmup) {} finish", ucpsiClient.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, UcpsiConfig config, int taskId,
                           Set<ByteBuffer> clientElementSet, int serverSetSize, PrintWriter printWriter)
        throws MpcAbortException {
        boolean silent = PropertiesUtils.readBoolean(properties, "silent");
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
            "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, true
        );
        UcpsiClient ucpsiClient = UcpsiFactory.createClient(clientRpc, serverParty, config);
        ucpsiClient.setTaskId(taskId);
        ucpsiClient.setParallel(true);
        // 启动测试
        ucpsiClient.getRpc().synchronize();
        ucpsiClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", ucpsiClient.ownParty().getPartyName());
        stopWatch.start();
        ucpsiClient.init(clientSetSize, serverSetSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = ucpsiClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = ucpsiClient.getRpc().getPayloadByteLength();
        long initSendByteLength = ucpsiClient.getRpc().getSendByteLength();
        ucpsiClient.getRpc().synchronize();
        ucpsiClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", ucpsiClient.ownParty().getPartyName());
        stopWatch.start();
        ucpsiClient.psi(clientElementSet);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = ucpsiClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = ucpsiClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = ucpsiClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = ucpsiClient.ownParty().getPartyId()
            + "\t" + clientSetSize
            + "\t" + serverSetSize
            + "\t" + ucpsiClient.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + silent
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        ucpsiClient.getRpc().synchronize();
        ucpsiClient.getRpc().reset();
        ucpsiClient.destroy();
        LOGGER.info("{} finish", ucpsiClient.ownParty().getPartyName());
    }
}

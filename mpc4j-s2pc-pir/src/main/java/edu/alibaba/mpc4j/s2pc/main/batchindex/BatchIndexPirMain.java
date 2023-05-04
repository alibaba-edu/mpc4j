package edu.alibaba.mpc4j.s2pc.main.batchindex;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Batch Index PIR主函数。
 *
 * @author Liqiang Peng
 * @date 2023/3/20
 */
public class BatchIndexPirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchIndexPirMain.class);
    /**
     * 任务名称
     */
    public static final String TASK_NAME = "BATCH_INDEX_PIR_TASK";
    /**
     * 协议类型名称
     */
    public static final String PTO_TYPE_NAME = "BATCH_INDEX_PIR";
    /**
     * 预热元素比特长度
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 16;
    /**
     * 预热服务端集合大小
     */
    private static final int WARMUP_SERVER_ELEMENT_SIZE = 1 << 10;
    /**
     * 预热客户端批检索数目
     */
    private static final int WARMUP_RETRIEVAL_SIZE = 1 << 2;
    /**
     * 秒表
     */
    private final StopWatch stopWatch;
    /**
     * 配置参数
     */
    private final Properties properties;

    public BatchIndexPirMain(Properties properties) {
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
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取元素比特长度
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        // 读取服务端集合大小
        int serverLogElementSize = PropertiesUtils.readInt(properties, "server_log_element_size");
        // 读取客户端批查询数目
        int[] clientLogRetrievalSize = PropertiesUtils.readLogIntArray(properties, "client_log_retrieval_size");
        int setSizeNum = clientLogRetrievalSize.length;
        int serverElementSize = 1 << serverLogElementSize;
        int[] clientRetrievalSize = Arrays.stream(clientLogRetrievalSize).map(logSize -> 1 << logSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        BatchIndexPirConfig config = BatchIndexPirConfigUtils.createBatchIndexPirConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(serverElementSize, elementBitLength);
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Element Size\tClient Retrieval Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 建立连接
        serverRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, config, taskId++);
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // 正式测试
            byte[][] serverElementArray = readServerElementArray(serverElementSize, elementBitLength);
            // 单线程
            runServer(serverRpc, clientParty, config, taskId++, false, serverElementArray,
                clientRetrievalSize[setSizeIndex], elementBitLength, printWriter);
            // 多线程
            runServer(serverRpc, clientParty, config, taskId++, true, serverElementArray,
                clientRetrievalSize[setSizeIndex], elementBitLength, printWriter);
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private byte[][] readServerElementArray(int elementSize, int elementBitLength) throws IOException {
        LOGGER.info("Server read element array");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PirUtils.getServerFileName(PirUtils.BYTES_SERVER_PREFIX, elementSize, elementBitLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        byte[][] elementArray = bufferedReader.lines()
            .map(Hex::decode)
            .toArray(byte[][]::new);
        bufferedReader.close();
        inputStreamReader.close();
        return elementArray;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, BatchIndexPirConfig config, int taskId)
        throws Exception {
        byte[][] serverElementArray = readServerElementArray(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        NaiveDatabase database = NaiveDatabase.create(WARMUP_ELEMENT_BIT_LENGTH, serverElementArray);
        BatchIndexPirServer server = BatchIndexPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(database, WARMUP_RETRIEVAL_SIZE);
        server.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", server.ownParty().getPartyName());
        server.pir();
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("(warmup) {} finish", server.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, BatchIndexPirConfig config, int taskId, boolean parallel,
                           byte[][] serverElementArray, int maxRetrievalSize, int elementBitLength,
                           PrintWriter printWriter) throws MpcAbortException {
        int serverElementSize = serverElementArray.length;
        LOGGER.info(
            "{}: serverElementSize = {}, maxRetrievalSize = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverElementSize, maxRetrievalSize, parallel
        );
        BatchIndexPirServer server = BatchIndexPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        NaiveDatabase database = NaiveDatabase.create(elementBitLength, serverElementArray);
        // 启动测试
        server.getRpc().synchronize();
        server.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(database, maxRetrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = server.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = server.getRpc().getPayloadByteLength();
        long initSendByteLength = server.getRpc().getSendByteLength();
        server.getRpc().synchronize();
        server.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        server.pir();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        // 写入统计结果
        String info = server.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + maxRetrievalSize
            + "\t" + server.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取元素比特长度
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        // 读取服务端集合大小
        int serverLogElementSize = PropertiesUtils.readInt(properties, "server_log_element_size");
        // 读取客户端批查询数目
        int[] clientLogRetrievalSize = PropertiesUtils.readLogIntArray(properties, "client_log_retrieval_size");
        int setSizeNum = clientLogRetrievalSize.length;
        int serverElementSize = 1 << serverLogElementSize;
        int[] clientRetrievalSize = Arrays.stream(clientLogRetrievalSize).map(logSize -> 1 << logSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        BatchIndexPirConfig config = BatchIndexPirConfigUtils.createBatchIndexPirConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_RETRIEVAL_SIZE);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateIndexInputFiles(serverElementSize, clientRetrievalSize[setSizeIndex]);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".txt";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Element Size\tClient Retrieval Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, config, taskId++);
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // 读取输入文件
            ArrayList<Integer> indexList = readClientRetrievalIndexList(clientRetrievalSize[setSizeIndex]);
            // 单线程
            runClient(clientRpc, serverParty, config, taskId++, false, indexList, serverElementSize, elementBitLength,
                printWriter);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId++, true, indexList, serverElementSize, elementBitLength,
                printWriter);
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private ArrayList<Integer> readClientRetrievalIndexList(int retrievalSize) throws IOException {
        LOGGER.info("Client read retrieval list");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PirUtils.getClientFileName(PirUtils.BYTES_CLIENT_PREFIX, retrievalSize)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ArrayList<Integer> indexList = bufferedReader.lines()
            .map(Hex::decode)
            .map(IntUtils::byteArrayToInt)
            .collect(Collectors.toCollection(ArrayList::new));
        bufferedReader.close();
        inputStreamReader.close();
        return indexList;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config, int taskId) throws Exception {
        ArrayList<Integer> retrievalIndexList = readClientRetrievalIndexList(WARMUP_RETRIEVAL_SIZE);
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_RETRIEVAL_SIZE);
        client.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        client.pir(retrievalIndexList);
        // 同步并等待5秒钟，保证对方执行完毕
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config, int taskId, boolean parallel,
                           ArrayList<Integer> clientIndexList, int serverElementSize, int elementBitLength,
                           PrintWriter printWriter) throws MpcAbortException {
        int retrievalSize = clientIndexList.size();
        LOGGER.info(
            "{}: serverElementSize = {}, retrievalSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverElementSize, retrievalSize, parallel
        );
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        // 启动测试
        client.getRpc().synchronize();
        client.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverElementSize, elementBitLength, retrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = client.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = client.getRpc().getPayloadByteLength();
        long initSendByteLength = client.getRpc().getSendByteLength();
        client.getRpc().synchronize();
        client.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        client.pir(clientIndexList);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = client.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = client.getRpc().getPayloadByteLength();
        long ptoSendByteLength = client.getRpc().getSendByteLength();
        // 写入统计结果
        String info = client.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + retrievalSize
            + "\t" + client.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("{} finish", client.ownParty().getPartyName());
    }
}

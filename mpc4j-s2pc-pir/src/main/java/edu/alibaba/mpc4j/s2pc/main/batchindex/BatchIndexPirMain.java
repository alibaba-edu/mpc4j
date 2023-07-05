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
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Batch Index PIR main.
 *
 * @author Liqiang Peng
 * @date 2023/3/20
 */
public class BatchIndexPirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchIndexPirMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "BATCH_INDEX_PIR_TASK";
    /**
     * protocol type
     */
    public static final String PTO_TYPE_NAME = "BATCH_INDEX_PIR";
    /**
     * warmup element bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 16;
    /**
     * warmup server element size
     */
    private static final int WARMUP_SERVER_ELEMENT_SIZE = 1 << 10;
    /**
     * warmup retrieval size
     */
    private static final int WARMUP_RETRIEVAL_SIZE = 1 << 2;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * properties
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
        // server reads properties
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // element bit length
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        // server log element size
        int serverLogElementSize = PropertiesUtils.readInt(properties, "server_log_element_size");
        // client log retrieval size
        int[] clientLogRetrievalSize = PropertiesUtils.readLogIntArray(properties, "client_log_retrieval_size");
        int setSizeNum = clientLogRetrievalSize.length;
        int serverElementSize = 1 << serverLogElementSize;
        int[] clientRetrievalSize = Arrays.stream(clientLogRetrievalSize).map(logSize -> 1 << logSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        BatchIndexPirConfig config = BatchIndexPirConfigUtils.createBatchIndexPirConfig(properties);
        // server generates input files
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(serverElementSize, elementBitLength);
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // server creates statistical result files
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // server writes statistical result files
        String tab = "Party ID\tServer Element Size\tClient Retrieval Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // connect
        serverRpc.connect();
        int taskId = 0;
        // warmup test
        warmupServer(serverRpc, clientParty, config, taskId++);
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // formal test
            byte[][] serverElementArray = readServerElementArray(serverElementSize, elementBitLength);
            // single thread
            runServer(serverRpc, clientParty, config, taskId++, false, serverElementArray,
                clientRetrievalSize[setSizeIndex], elementBitLength, printWriter);
            // multi thread
            runServer(serverRpc, clientParty, config, taskId++, true, serverElementArray,
                clientRetrievalSize[setSizeIndex], elementBitLength, printWriter);
        }
        // disconnect
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
        // init protocol
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(database, WARMUP_RETRIEVAL_SIZE);
        server.getRpc().synchronize();
        // execute protocol
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
        server.getRpc().synchronize();
        server.getRpc().reset();
        // init protocol
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
        // execute protocol
        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        server.pir();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        // write statistical result files
        String info = server.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + maxRetrievalSize
            + "\t" + server.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // synchronize
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        // client reads properties
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // element bit length
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
        // server log element size
        int serverLogElementSize = PropertiesUtils.readInt(properties, "server_log_element_size");
        // client log retrieval size
        int[] clientLogRetrievalSize = PropertiesUtils.readLogIntArray(properties, "client_log_retrieval_size");
        int setSizeNum = clientLogRetrievalSize.length;
        int serverElementSize = 1 << serverLogElementSize;
        int[] clientRetrievalSize = Arrays.stream(clientLogRetrievalSize).map(logSize -> 1 << logSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        BatchIndexPirConfig config = BatchIndexPirConfigUtils.createBatchIndexPirConfig(properties);
        // client generates input files
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_RETRIEVAL_SIZE);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateIndexInputFiles(serverElementSize, clientRetrievalSize[setSizeIndex]);
        }
        // client creates statistical result files
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // client writes statistical result files
        String tab = "Party ID\tServer Element Size\tClient Retrieval Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // connect
        clientRpc.connect();
        int taskId = 0;
        // warmup test
        warmupClient(clientRpc, serverParty, config, taskId++);
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            // formal test
            List<Integer> indexList = readClientRetrievalIndexList(clientRetrievalSize[setSizeIndex]);
            // single thread
            runClient(clientRpc, serverParty, config, taskId++, false, indexList, serverElementSize, elementBitLength,
                printWriter);
            // multi thread
            runClient(clientRpc, serverParty, config, taskId++, true, indexList, serverElementSize, elementBitLength,
                printWriter);
        }
        // disconnect
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private List<Integer> readClientRetrievalIndexList(int retrievalSize) throws IOException {
        LOGGER.info("Client read retrieval list");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PirUtils.getClientFileName(PirUtils.BYTES_CLIENT_PREFIX, retrievalSize)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        List<Integer> indexList = bufferedReader.lines()
            .map(Hex::decode)
            .map(IntUtils::byteArrayToInt)
            .collect(Collectors.toCollection(ArrayList::new));
        bufferedReader.close();
        inputStreamReader.close();
        return indexList;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config, int taskId) throws Exception {
        List<Integer> retrievalIndexList = readClientRetrievalIndexList(WARMUP_RETRIEVAL_SIZE);
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        // init protocol
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_RETRIEVAL_SIZE);
        client.getRpc().synchronize();
        // execute protocol
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        client.pir(retrievalIndexList);
        // synchronize
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config, int taskId, boolean parallel,
                           List<Integer> clientIndexList, int serverElementSize, int elementBitLength,
                           PrintWriter printWriter) throws MpcAbortException {
        int retrievalSize = clientIndexList.size();
        LOGGER.info(
            "{}: serverElementSize = {}, retrievalSize = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverElementSize, retrievalSize, parallel
        );
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        // init protocol
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
        // execute protocol
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        client.pir(clientIndexList);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = client.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = client.getRpc().getPayloadByteLength();
        long ptoSendByteLength = client.getRpc().getSendByteLength();
        // write statistical result files
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

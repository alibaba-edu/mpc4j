package edu.alibaba.mpc4j.s2pc.pir.main.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleCpPirServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * client-specific preprocessing PIR main.
 *
 * @author Liqiang Peng
 * @date 2023/9/26
 */
public class SingleCpPirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCpPirMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "SINGLE_CP_PIR_TASK";
    /**
     * protocol name
     */
    private static final String PTO_TYPE_NAME = "SINGLE_CP_PIR_PTO";
    /**
     * warmup entry bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 16;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * warmup query num
     */
    private static final int WARMUP_QUERY_NUM = 1 << 5;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * properties
     */
    private final Properties properties;

    public SingleCpPirMain(Properties properties) {
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
            throw new IllegalArgumentException(
                "Invalid PartyID for own_name " + ownRpc.ownParty().getPartyName() + ": " + ownRpc.ownParty().getPartyId()
            );
        }
    }

    private void runServer(Rpc serverRpc, Party clientParty) throws Exception {
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        int entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        boolean parallel = PropertiesUtils.readBoolean(properties, "parallel");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int queryNum = PropertiesUtils.readInt(properties, "query_num");
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        SingleCpPirConfig config = SingleCpPirConfigUtils.createIndexCpPirConfig(properties);
        LOGGER.info("{} generate warm-up database file", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate database file", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], entryBitLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + entryBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        serverRpc.connect();
        int taskId = 0;
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            ZlDatabase database = readServerDatabase(serverSetSize, entryBitLength);
            runServer(serverRpc, clientParty, config, taskId, parallel, database, queryNum, printWriter);
            taskId++;
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private ZlDatabase readServerDatabase(int n, int entryBitLength) throws IOException {
        LOGGER.info("Server read database");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(PirUtils.getServerFileName(PirUtils.BYTES_SERVER_PREFIX, n, entryBitLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        byte[][] entries = bufferedReader.lines()
            .map(Hex::decode)
            .toArray(byte[][]::new);
        bufferedReader.close();
        inputStreamReader.close();
        return ZlDatabase.create(entryBitLength, entries);
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, SingleCpPirConfig config, int taskId)
        throws Exception {
        ZlDatabase database = readServerDatabase(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), database.rows(), database.getL(), WARMUP_QUERY_NUM, false
        );
        SingleCpPirServer server = SingleCpPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(database);
        server.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", server.ownParty().getPartyName());
        for (int i = 0; i < WARMUP_QUERY_NUM; i++) {
            server.pir();
        }
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("(warmup) {} finish", server.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, SingleCpPirConfig config, int taskId,
                           boolean parallel, ZlDatabase database, int queryNum, PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), database.rows(), database.getL(), queryNum, parallel
        );
        SingleCpPirServer server = SingleCpPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(database);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = server.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = server.getRpc().getPayloadByteLength();
        long initSendByteLength = server.getRpc().getSendByteLength();
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        for (int i = 0; i < queryNum; i++) {
            server.pir();
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        String info = server.ownParty().getPartyId()
            + "\t" + database.rows()
            + "\t" + queryNum
            + "\t" + server.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        int entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        boolean parallel = PropertiesUtils.readBoolean(properties, "parallel");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int queryNum = PropertiesUtils.readInt(properties, "query_num");
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        SingleCpPirConfig config = SingleCpPirConfigUtils.createIndexCpPirConfig(properties);
        LOGGER.info("{} generate warm-up index file", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_QUERY_NUM);
        LOGGER.info("{} generate index file", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateIndexInputFiles(serverSetSizes[setSizeIndex], queryNum);
        }
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + entryBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)\t Memory";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            List<Integer> indexList = readClientRetrievalIndexList(queryNum);
            runClient(clientRpc, serverParty, config, taskId, indexList, serverSetSize, entryBitLength, parallel, printWriter);
            taskId++;
        }
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

    private void warmupClient(Rpc clientRpc, Party serverParty, SingleCpPirConfig config, int taskId)
        throws Exception {
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM,
            false
        );
        List<Integer> indexList = readClientRetrievalIndexList(WARMUP_QUERY_NUM);
        SingleCpPirClient client = SingleCpPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        for (int i = 0; i < WARMUP_QUERY_NUM; i++) {
            client.pir(indexList.get(i));
        }
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, SingleCpPirConfig config, int taskId,
                           List<Integer> indexList, int serverSetSize, int entryBitLength, boolean parallel,
                           PrintWriter printWriter)
        throws MpcAbortException {
        int queryNum = indexList.size();
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, entryBitLength, queryNum, parallel
        );
        SingleCpPirClient client = SingleCpPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverSetSize, entryBitLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long memory = GraphLayout.parseInstance(client).totalSize();
        long initDataPacketNum = client.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = client.getRpc().getPayloadByteLength();
        long initSendByteLength = client.getRpc().getSendByteLength();
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        for (Integer integer : indexList) {
            client.pir(integer);
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = client.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = client.getRpc().getPayloadByteLength();
        long ptoSendByteLength = client.getRpc().getSendByteLength();
        String info = client.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + queryNum
            + "\t" + client.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
            + "\t" + memory;
        printWriter.println(info);
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("{} finish", client.ownParty().getPartyName());
    }
}

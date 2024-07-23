package edu.alibaba.mpc4j.s2pc.pir.main.kspir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirServer;
import org.bouncycastle.util.encoders.Hex;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Single Keyword PIR main.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
public class SingleKsPirMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleKsPirMain.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "single_kw_pir_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "SINGLE_KW_PIR";
    /**
     * warmup element bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 16;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * warmup query number
     */
    private static final int WARMUP_QUERY_NUM = 3;
    /**
     * entry bit length
     */
    private final int entryBitLength;
    /**
     * parallel
     */
    private final boolean parallel;
    /**
     * server set sizes
     */
    private final int[] serverSetSizes;
    /**
     * server set size num
     */
    private final int serverSetSizeNum;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * config
     */
    private final StdKsPirConfig config;

    public SingleKsPirMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read common config
        LOGGER.info("{} read common config", ownRpc.ownParty().getPartyName());
        entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        parallel = PropertiesUtils.readBoolean(properties, "parallel");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        serverSetSizeNum = serverLogSetSizes.length;
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        // read PTO config
        LOGGER.info("{} read PTO config", ownRpc.ownParty().getPartyName());
        config = SingleKsPirConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int i = 0 ; i < serverSetSizeNum; i++) {
            PirUtils.generateBytesInputFiles(serverSetSizes[i], entryBitLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Number\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        serverRpc.connect();
        int taskId = 0;
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        for (int i = 0; i < serverSetSizeNum; i++) {
            int serverSetSize = serverSetSizes[i];
            byte[][] elementArray = readServerElementArray(serverSetSize, entryBitLength);
            runServer(serverRpc, clientParty, config, taskId, parallel, elementArray, entryBitLength, queryNum, printWriter);
            taskId++;
        }
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

    private void warmupServer(Rpc serverRpc, Party clientParty, StdKsPirConfig config, int taskId)
        throws IOException, MpcAbortException {
        byte[][] elementArray = readServerElementArray(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, WARMUP_SERVER_SET_SIZE)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> elementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(WARMUP_SERVER_SET_SIZE)
            ));
        LOGGER.info(
            "(warmup) {}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM,
            false
        );
        StdKsPirServer<ByteBuffer> server = StdKsPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(keyValueMap, byteL * Byte.SIZE, 1);
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

    private void runServer(Rpc serverRpc, Party clientParty, StdKsPirConfig config, int taskId, boolean parallel,
                           byte[][] elementArray, int elementBitLength, int queryNum, PrintWriter printWriter)
        throws MpcAbortException {
        int byteL = CommonUtils.getByteLength(elementBitLength);
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), elementArray.length, elementBitLength, queryNum, parallel
        );
        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, elementArray.length)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> elementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(elementArray.length)
            ));
        StdKsPirServer<ByteBuffer> server = StdKsPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(keyValueMap, byteL * Byte.SIZE, 1);
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
            + "\t" + elementArray.length
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

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws IOException, MpcAbortException {
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_QUERY_NUM);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int i = 0; i < serverSetSizeNum; i++) {
            PirUtils.generateIndexInputFiles(serverSetSizes[i], queryNum);
        }
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Number\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)\tMemory";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int i = 0; i < serverSetSizeNum; i++) {
            int serverSetSize = serverSetSizes[i];
            int[] index = readClientRetrievalIndexList(queryNum).stream().mapToInt(query -> query).toArray();
            runClient(clientRpc, serverParty, config, taskId, index, serverSetSize, entryBitLength, parallel, printWriter);
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

    private void warmupClient(Rpc clientRpc, Party serverParty, StdKsPirConfig config, int taskId)
        throws IOException, MpcAbortException {
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info(
            "(warmup) {}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM,
            false
        );
        int[] index = readClientRetrievalIndexList(WARMUP_QUERY_NUM).stream().mapToInt(i -> i).toArray();
        StdKsPirClient<ByteBuffer> client = StdKsPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SERVER_SET_SIZE, byteL * Byte.SIZE, 1);
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        for (int i = 0; i < WARMUP_QUERY_NUM; i++) {
            Set<ByteBuffer> retrievalKey = new HashSet<>(1);
            retrievalKey.add(ByteBuffer.wrap(IntUtils.intToByteArray(index[i])));
            client.pir(new ArrayList<>(retrievalKey));
        }
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, StdKsPirConfig config, int taskId,
                           int[] index, int serverSetSize, int elementBitLength, boolean parallel,
                           PrintWriter printWriter)
        throws MpcAbortException {
        int queryNum = index.length;
        int byteL = CommonUtils.getByteLength(elementBitLength);
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, elementBitLength, queryNum, parallel
        );
        StdKsPirClient<ByteBuffer> client = StdKsPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverSetSize, byteL * Byte.SIZE, 1);
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
        for (int j : index) {
            Set<ByteBuffer> retrievalKey = new HashSet<>(1);
            retrievalKey.add(ByteBuffer.wrap(IntUtils.intToByteArray(j)));
            client.pir(new ArrayList<>(retrievalKey));
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

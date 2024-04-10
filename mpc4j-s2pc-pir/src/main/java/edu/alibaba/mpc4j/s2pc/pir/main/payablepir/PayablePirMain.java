package edu.alibaba.mpc4j.s2pc.pir.main.payablepir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirClient;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirFactory;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirServer;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
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
 * Payable PIR main.
 *
 * @author Liqiang Peng
 * @date 2023/9/28
 */
public class PayablePirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePirMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "PAYABLE_PIR_TASK";
    /**
     * protocol name
     */
    public static final String PTO_TYPE_NAME = "PAYABLE_PIR_TASK";
    /**
     * warmup element bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 10;
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

    public PayablePirMain(Properties properties) {
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
        boolean parallel = PropertiesUtils.readBoolean(properties, "parallel");
        // server log element size
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int queryNum = PropertiesUtils.readInt(properties, "query_number");
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        PayablePirConfig config = PayablePirConfigUtils.createPayablePirConfig(properties);
        // server generates input files
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], elementBitLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // server creates statistical result files
        String filePath = PTO_TYPE_NAME
            + "_" + config.getProType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // server writes statistical result files
        String tab = "Party ID\tServer Element Size\tClient Query Number\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // connect
        serverRpc.connect();
        int taskId = 0;
        // warmup test
        warmupServer(serverRpc, clientParty, config, taskId++);
        // formal test multi thread
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            byte[][] serverElementArray = readServerElementArray(serverSetSize, elementBitLength);
            runServer(serverRpc, clientParty, config, taskId++, serverElementArray, elementBitLength, queryNum,
                parallel, printWriter);
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

    private void warmupServer(Rpc serverRpc, Party clientParty, PayablePirConfig config, int taskId)
        throws Exception {
        byte[][] elementArray = readServerElementArray(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        Map<ByteBuffer, byte[]> keywordValueMap = IntStream.range(0, WARMUP_SERVER_ELEMENT_SIZE)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> elementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(WARMUP_SERVER_ELEMENT_SIZE)
            ));
        PayablePirServer server = PayablePirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        // init protocol
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(keywordValueMap, byteL);
        server.getRpc().synchronize();
        // execute protocol
        LOGGER.info("(warmup) {} execute", server.ownParty().getPartyName());
        for (int i = 0; i < WARMUP_RETRIEVAL_SIZE; i++) {
            server.pir();
        }
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("(warmup) {} finish", server.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, PayablePirConfig config, int taskId,
                           byte[][] serverElementArray, int elementBitLength, int queryNum, boolean parallel,
                           PrintWriter printWriter) throws MpcAbortException {
        int byteL = CommonUtils.getByteLength(elementBitLength);
        int serverElementSize = serverElementArray.length;
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), serverElementSize, elementBitLength, queryNum, parallel
        );
        Map<ByteBuffer, byte[]> keywordValueMap = IntStream.range(0, serverElementSize)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> serverElementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(serverElementSize)
            ));
        PayablePirServer server = PayablePirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        // init protocol
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(keywordValueMap, byteL);
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
        for (int i = 0; i < queryNum; i++) {
            server.pir();
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        // write statistical result files
        String info = server.ownParty().getPartyId()
            + "\t" + serverElementSize
            + "\t" + queryNum
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
        boolean parallel = PropertiesUtils.readBoolean(properties, "parallel");
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int queryNum = PropertiesUtils.readInt(properties, "query_number");
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        PayablePirConfig config = PayablePirConfigUtils.createPayablePirConfig(properties);
        // client generates input files
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_ELEMENT_SIZE, WARMUP_RETRIEVAL_SIZE);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateIndexInputFiles(serverSetSizes[setSizeIndex], queryNum);
        }
        // client creates statistical result files
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
            + "_" + config.getProType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // client writes statistical result files
        String tab = "Party ID\tServer Element Size\tClient Query Number\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // connect
        clientRpc.connect();
        int taskId = 0;
        // warmup test
        warmupClient(clientRpc, serverParty, config, taskId++);
        // formal test multi thread
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int[] index = readClientRetrievalIndexList(queryNum).stream().mapToInt(i -> i).toArray();
            runClient(clientRpc, serverParty, config, taskId++, index, serverSetSize, elementBitLength, parallel, printWriter);
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

    private void warmupClient(Rpc clientRpc, Party serverParty, PayablePirConfig config, int taskId) throws Exception {
        int byteL = CommonUtils.getByteLength(WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), WARMUP_SERVER_ELEMENT_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_RETRIEVAL_SIZE,
            false
        );
        int[] index = readClientRetrievalIndexList(WARMUP_RETRIEVAL_SIZE).stream().mapToInt(i -> i).toArray();
        PayablePirClient client = PayablePirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        // init protocol
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(WARMUP_SERVER_ELEMENT_SIZE, byteL);
        client.getRpc().synchronize();
        // execute protocol
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        for (int i = 0; i < WARMUP_RETRIEVAL_SIZE; i++) {
            client.pir(ByteBuffer.wrap(IntUtils.intToByteArray(index[i])));
        }
        // synchronize
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, PayablePirConfig config, int taskId,
                           int[] index, int serverElementSize, int elementBitLength, boolean parallel,
                           PrintWriter printWriter) throws MpcAbortException {
        int retrievalSize = index.length;
        int byteL = CommonUtils.getByteLength(elementBitLength);
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverElementSize, elementBitLength, retrievalSize, parallel
        );
        PayablePirClient client = PayablePirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        // init protocol
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverElementSize, byteL);
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
        for (int j : index) {
            client.pir(ByteBuffer.wrap(IntUtils.intToByteArray(j)));
        }
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

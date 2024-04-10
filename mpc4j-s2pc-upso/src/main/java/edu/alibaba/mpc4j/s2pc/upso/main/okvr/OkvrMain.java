package edu.alibaba.mpc4j.s2pc.upso.main.okvr;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrFactory;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.OkvrSender;
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
 * OKVR main.
 *
 * @author Liqiang Peng
 * @date 2023/4/23
 */
public class OkvrMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(OkvrMain.class);
    /**
     * task name
     */
    public static final String TASK_NAME = "OKVR_TASK";
    /**
     * warmup element bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = 64;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * warmup client set size
     */
    private static final int WARMUP_CLIENT_SET_SIZE = 1 << 5;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * properties
     */
    private final Properties properties;

    public OkvrMain(Properties properties) {
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
        String okvrTypeString = PropertiesUtils.readString(properties, "pto_name");
        OkvrType okvrType = OkvrType.valueOf(okvrTypeString);
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
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
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        OkvrConfig config = OkvrConfigUtils.createOkvrConfig(properties);
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PirUtils.generateBytesInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], elementBitLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = okvrType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
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
            byte[][] elementArray = readServerElementArray(serverSetSize, elementBitLength);
            runServer(
                serverRpc, clientParty, config, taskId, elementArray, elementBitLength, clientSetSizes[setSizeIndex], printWriter
            );
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

    private void warmupServer(Rpc serverRpc, Party clientParty, OkvrConfig config, int taskId) throws Exception {
        byte[][] elementArray = readServerElementArray(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH);
        Map<ByteBuffer, byte[]> keywordValueMap = IntStream.range(0, WARMUP_SERVER_SET_SIZE)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> elementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(WARMUP_SERVER_SET_SIZE)
            ));
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_CLIENT_SET_SIZE,
            false
        );
        OkvrSender okvrSender = OkvrFactory.createSender(serverRpc, clientParty, config);
        okvrSender.setTaskId(taskId);
        okvrSender.setParallel(false);
        okvrSender.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", okvrSender.ownParty().getPartyName());
        okvrSender.init(keywordValueMap, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_CLIENT_SET_SIZE);
        okvrSender.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", okvrSender.ownParty().getPartyName());
        okvrSender.okvr();
        okvrSender.getRpc().synchronize();
        okvrSender.getRpc().reset();
        okvrSender.destroy();
        LOGGER.info("(warmup) {} finish", okvrSender.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, OkvrConfig config, int taskId, byte[][] elementArray,
                           int elementBitLength, int queryNum, PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), elementArray.length, elementBitLength, queryNum, true
        );
        Map<ByteBuffer, byte[]> keywordValueMap = IntStream.range(0, elementArray.length)
            .boxed()
            .collect(Collectors.toMap(
                i -> ByteBuffer.wrap(IntUtils.intToByteArray(i)),
                i -> elementArray[i],
                (a, b) -> b,
                () -> new HashMap<>(elementArray.length)
            ));
        OkvrSender okvrSender = OkvrFactory.createSender(serverRpc, clientParty, config);
        okvrSender.setTaskId(taskId);
        okvrSender.setParallel(true);
        okvrSender.getRpc().synchronize();
        okvrSender.getRpc().reset();
        LOGGER.info("{} init", okvrSender.ownParty().getPartyName());
        stopWatch.start();
        okvrSender.init(keywordValueMap, elementBitLength, queryNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = okvrSender.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = okvrSender.getRpc().getPayloadByteLength();
        long initSendByteLength = okvrSender.getRpc().getSendByteLength();
        okvrSender.getRpc().synchronize();
        okvrSender.getRpc().reset();
        LOGGER.info("{} execute", okvrSender.ownParty().getPartyName());
        stopWatch.start();
        okvrSender.okvr();
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = okvrSender.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = okvrSender.getRpc().getPayloadByteLength();
        long ptoSendByteLength = okvrSender.getRpc().getSendByteLength();
        String info = okvrSender.ownParty().getPartyId()
            + "\t" + elementArray.length
            + "\t" + queryNum
            + "\t" + okvrSender.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        okvrSender.getRpc().synchronize();
        okvrSender.getRpc().reset();
        okvrSender.destroy();
        LOGGER.info("{} finish", okvrSender.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty) throws Exception {
        String okvrTypeString = PropertiesUtils.readString(properties, "pto_name");
        OkvrType ucpsiType = OkvrType.valueOf(okvrTypeString);
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        int elementBitLength = PropertiesUtils.readInt(properties, "element_bit_length");
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
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        OkvrConfig config = OkvrConfigUtils.createOkvrConfig(properties);
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PirUtils.generateIndexInputFiles(WARMUP_SERVER_SET_SIZE, WARMUP_CLIENT_SET_SIZE);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PirUtils.generateIndexInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex]);
        }
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = ucpsiType.name()
            + "_" + config.getPtoType().name()
            + "_" + elementBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int[] index = readClientRetrievalIndexList(clientSetSizes[setSizeIndex]).stream().mapToInt(i -> i).toArray();
            runClient(clientRpc, serverParty, config, taskId, index, serverSetSize, elementBitLength, printWriter);
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

    private void warmupClient(Rpc clientRpc, Party serverParty, OkvrConfig config, int taskId) throws Exception {
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_CLIENT_SET_SIZE,
            false
        );
        int[] index = readClientRetrievalIndexList(WARMUP_CLIENT_SET_SIZE).stream().mapToInt(i -> i).toArray();
        OkvrReceiver okvrReceiver = OkvrFactory.createReceiver(clientRpc, serverParty, config);
        okvrReceiver.setTaskId(taskId);
        okvrReceiver.setParallel(false);
        okvrReceiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", okvrReceiver.ownParty().getPartyName());
        okvrReceiver.init(WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_CLIENT_SET_SIZE);
        okvrReceiver.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", okvrReceiver.ownParty().getPartyName());
        Set<ByteBuffer> retrievalKey = IntStream.range(0, WARMUP_CLIENT_SET_SIZE)
            .mapToObj(i -> ByteBuffer.wrap(IntUtils.intToByteArray(index[i])))
            .collect(Collectors.toCollection(() -> new HashSet<>(WARMUP_CLIENT_SET_SIZE)));
        okvrReceiver.okvr(retrievalKey);
        okvrReceiver.getRpc().synchronize();
        okvrReceiver.getRpc().reset();
        okvrReceiver.destroy();
        LOGGER.info("(warmup) {} finish", okvrReceiver.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, OkvrConfig config, int taskId,
                           int[] index, int serverSetSize, int elementBitLength, PrintWriter printWriter)
        throws MpcAbortException {
        int queryNum = index.length;
        LOGGER.info(
            "{}: serverSetSize = {}, elementBitLength = {}, queryNumber = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, elementBitLength, queryNum, true
        );
        OkvrReceiver okvrReceiver = OkvrFactory.createReceiver(clientRpc, serverParty, config);
        okvrReceiver.setTaskId(taskId);
        okvrReceiver.setParallel(true);
        okvrReceiver.getRpc().synchronize();
        okvrReceiver.getRpc().reset();
        LOGGER.info("{} init", okvrReceiver.ownParty().getPartyName());
        stopWatch.start();
        okvrReceiver.init(serverSetSize, elementBitLength, queryNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = okvrReceiver.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = okvrReceiver.getRpc().getPayloadByteLength();
        long initSendByteLength = okvrReceiver.getRpc().getSendByteLength();
        okvrReceiver.getRpc().synchronize();
        okvrReceiver.getRpc().reset();
        LOGGER.info("{} execute", okvrReceiver.ownParty().getPartyName());
        stopWatch.start();
        Set<ByteBuffer> retrievalKey = IntStream.range(0, queryNum)
            .mapToObj(i -> ByteBuffer.wrap(IntUtils.intToByteArray(index[i])))
            .collect(Collectors.toCollection(() -> new HashSet<>(queryNum)));
        okvrReceiver.okvr(retrievalKey);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = okvrReceiver.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = okvrReceiver.getRpc().getPayloadByteLength();
        long ptoSendByteLength = okvrReceiver.getRpc().getSendByteLength();
        String info = okvrReceiver.ownParty().getPartyId()
            + "\t" + queryNum
            + "\t" + serverSetSize
            + "\t" + okvrReceiver.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        okvrReceiver.getRpc().synchronize();
        okvrReceiver.getRpc().reset();
        okvrReceiver.destroy();
        LOGGER.info("{} finish", okvrReceiver.ownParty().getPartyName());
    }
}

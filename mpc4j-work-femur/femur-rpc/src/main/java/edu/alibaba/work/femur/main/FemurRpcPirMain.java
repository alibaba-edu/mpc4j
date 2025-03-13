package edu.alibaba.work.femur.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.work.femur.FemurRpcPirClient;
import edu.alibaba.work.femur.FemurRpcPirConfig;
import edu.alibaba.work.femur.FemurRpcPirFactory;
import edu.alibaba.work.femur.FemurRpcPirServer;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PGM-index range keyword PIR main.
 *
 * @author Liqiang Peng
 * @date 2024/9/18
 */
public class FemurRpcPirMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurRpcPirMain.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "range_kw_pir_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "RANGE_KW_PGM_PIR";
    /**
     * warmup element bit length
     */
    private static final int WARMUP_ELEMENT_BIT_LENGTH = Long.SIZE;
    /**
     * warmup server set size
     */
    private static final int WARMUP_SERVER_SET_SIZE = 1 << 10;
    /**
     * warmup range bound
     */
    private static final int WARMUP_RANGE_BOUND = 1 << 8;
    /**
     * warmup epsilon
     */
    private static final double WARMUP_EPSILON = 0.1;
    /**
     * warmup query number
     */
    private static final int WARMUP_QUERY_NUM = 1 << 5;
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
    private final FemurRpcPirConfig config;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * range bound
     */
    private final int rangeBound;
    /**
     * epsilon
     */
    private final double epsilon;
    /**
     * path name
     */
    private final String pathName;


    public FemurRpcPirMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read common config
        LOGGER.info("{} read common config", ownRpc.ownParty().getPartyName());
        entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        parallel = PropertiesUtils.readBoolean(properties, "parallel");
        serverSetSizes = PropertiesUtils.readIntArray(properties, "server_set_size");
        serverSetSizeNum = serverSetSizes.length;
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        rangeBound = PropertiesUtils.readInt(properties, "range_bound");
        epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        boolean dp = PropertiesUtils.readBoolean(properties, "differential_privacy");
        pathName = PropertiesUtils.readString(properties, "path_name");
        // read PTO config
        LOGGER.info("{} read PTO config", ownRpc.ownParty().getPartyName());
        config = FemurRpcPirConfigUtils.createConfig(properties, dp);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
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
            long[] elementArray = readElementArray(pathName, serverSetSize);
            runServer(serverRpc, clientParty, config, taskId, parallel, elementArray, entryBitLength, queryNum, printWriter);
            taskId++;
        }
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private long[] readElementArray(String pathName, int serverSetSize) throws IOException, MpcAbortException {
        LOGGER.info("read element array");
        File fileName = new File(pathName);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
        byte[] bytes = new byte[Long.BYTES];
        MpcAbortPreconditions.checkArgument(in.read(bytes) != -1);
        byte[] reverseBytes = BytesUtils.reverseByteArray(bytes);
        long num = LongUtils.byteArrayToLong(reverseBytes);
        MathPreconditions.checkGreaterOrEqual("num", num, serverSetSize);
        long[] array = new long[serverSetSize];
        for (int i = 0; i < serverSetSize; i++) {
            MpcAbortPreconditions.checkArgument(in.read(bytes) != -1);
            reverseBytes = BytesUtils.reverseByteArray(bytes);
            array[i] = LongUtils.byteArrayToLong(reverseBytes);
        }
        in.close();
        return array;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, FemurRpcPirConfig config, int taskId)
        throws IOException, MpcAbortException {
        long[] elementArray = readElementArray(pathName, WARMUP_SERVER_SET_SIZE);
        TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
        for (int i = 0; i < WARMUP_SERVER_SET_SIZE; i++) {
            keyValueMap.put(elementArray[i], LongUtils.longToByteArray(i));
        }
        LOGGER.info(
            "(warmup) {}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM,
            false
        );
        FemurRpcPirServer server = FemurRpcPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(false);
        server.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", server.ownParty().getPartyName());
        server.init(keyValueMap, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM);
        server.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", server.ownParty().getPartyName());
        server.pir(WARMUP_QUERY_NUM);
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("(warmup) {} finish", server.ownParty().getPartyName());
    }

    private void runServer(Rpc serverRpc, Party clientParty, FemurRpcPirConfig config, int taskId, boolean parallel,
                           long[] elementArray, int elementBitLength, int queryNum, PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), elementArray.length, elementBitLength, queryNum, parallel
        );
        TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
        for (int i = 0; i < elementArray.length; i++) {
            keyValueMap.put(elementArray[i], LongUtils.longToByteArray(i));
        }
        FemurRpcPirServer server = FemurRpcPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(keyValueMap, elementBitLength, queryNum);
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
        server.pir(queryNum);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        String info = server.ownParty().getPartyId()
            + "\t" + keyValueMap.size()
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
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + "_" + clientRpc.ownParty().getPartyId()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Number\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int i = 0; i < serverSetSizeNum; i++) {
            int serverSetSize = serverSetSizes[i];
            long[] index = readElementArray(pathName, serverSetSize);
            long[] retrievalIndex = IntStream.range(0, queryNum)
                .mapToLong(j -> index[SECURE_RANDOM.nextInt(serverSetSize)])
                .toArray();
            runClient(clientRpc,
                serverParty,
                config,
                taskId,
                retrievalIndex,
                Math.toIntExact(Arrays.stream(index).distinct().count()),
                entryBitLength,
                parallel,
                printWriter);
            taskId++;
        }
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, FemurRpcPirConfig config, int taskId)
        throws IOException, MpcAbortException {
        LOGGER.info(
            "(warmup) {}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), WARMUP_SERVER_SET_SIZE, WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM,
            false
        );
        long[] index = readElementArray(pathName, WARMUP_SERVER_SET_SIZE);
        long[] retrievalIndex = IntStream.range(0, WARMUP_QUERY_NUM)
            .mapToLong(i -> index[SECURE_RANDOM.nextInt(WARMUP_SERVER_SET_SIZE)])
            .toArray();
        FemurRpcPirClient client = FemurRpcPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(false);
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} init", client.ownParty().getPartyName());
        client.init(
            Math.toIntExact(Arrays.stream(index).distinct().count()), WARMUP_ELEMENT_BIT_LENGTH, WARMUP_QUERY_NUM
        );
        client.getRpc().synchronize();
        LOGGER.info("(warmup) {} execute", client.ownParty().getPartyName());
        client.pir(retrievalIndex, WARMUP_RANGE_BOUND, WARMUP_EPSILON);
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("(warmup) {} finish", client.ownParty().getPartyName());
    }

    private void runClient(Rpc clientRpc, Party serverParty, FemurRpcPirConfig config, int taskId,
                           long[] index, int serverSetSize, int elementBitLength, boolean parallel,
                           PrintWriter printWriter)
        throws MpcAbortException {
        int queryNum = index.length;
        LOGGER.info(
            "{}: serverSetSize = {}, entryBitLength = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), serverSetSize, elementBitLength, queryNum, parallel
        );
        FemurRpcPirClient client = FemurRpcPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverSetSize, elementBitLength, queryNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = client.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = client.getRpc().getPayloadByteLength();
        long initSendByteLength = client.getRpc().getSendByteLength();
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        client.pir(index, rangeBound, epsilon);
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
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("{} finish", client.ownParty().getPartyName());
    }
}

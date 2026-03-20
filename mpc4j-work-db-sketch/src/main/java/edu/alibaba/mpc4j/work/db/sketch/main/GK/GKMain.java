package edu.alibaba.mpc4j.work.db.sketch.main.GK;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKConfig;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKFactory;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKParty;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKTable;
import edu.alibaba.mpc4j.work.db.sketch.utils.DataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
/**
 * Greenwald-Khanna (GK) sketch experiment main class.
 * <p>
 * This class implements the experiment runner for the GK sketch protocol in a 3PC environment.
 * GK is a quantile sketch algorithm for approximate quantile queries on streaming data.
 * The experiment measures performance metrics including initialization time, protocol execution time,
 * communication overhead, and supports both parallel and sequential execution.
 * </p>
 */
public class GKMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GKMain.class);
    /**
     * Protocol type name identifier
     */
    public static final String PTO_TYPE = "GK";
    /**
     * Configuration key for protocol type
     */
    public static final String PTO_NAME_KEY = "gk_pto_name";
    
    // Warmup configuration constants
    /**
     * Logarithm of sketch table size for warmup phase
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 10;
    /**
     * Key bit length for warmup phase
     */
    private static final int WARMUP_KEY_BIT_LEN = 20;
    /**
     * Payload bit length for warmup phase
     */
    private static final int WARMUP_PAYLOAD_BIT_LEN = 12;
    /**
     * Update count for warmup phase
     */
    private static final int WARMUP_UPDATE_NUM = 1 << 12;
    /**
     * Epsilon parameter for warmup phase (error tolerance)
     */
    private static final double WARMUP_EPSILON = 0.1;
    /**
     * Query frequency for warmup phase
     */
    private static final int WARMUP_QUERY = 100;
    
    // Experiment configuration parameters
    /**
     * Logarithm of sketch table sizes for each experiment
     */
    private final int[] logSketchSizes;
    /**
     * Logarithm of update data sizes for each experiment
     */
    private final int[] logUpdateSizes;
    /**
     * Key bit lengths for each experiment
     */
    private final int[] keyBitLen;
    /**
     * Payload bit lengths for each experiment
     */
    private final int[] payloadBitLen;
    /**
     * Epsilon parameters (error tolerance) for each experiment
     */
    private final double[] epsilons;
    /**
     * Query frequencies for each experiment
     */
    private final int[] queryFrequencies;
    /**
     * GK protocol configuration
     */
    private final GKConfig config;
    /**
     * Random seed keys for deterministic data generation
     */
    private byte[] randKeys;

    private final DataGenerator dataGenerator = new DataGenerator();
    /**
     * Type of data distribution for experiments
     */
    private final String dataType = "UNIFORM";

    /**
     * Generates update row data as shared Z2 vectors.
     * <p>
     * Creates public values for all elements in the sketch table using a shared
     * random seed to ensure all parties have the same data.
     * </p>
     *
     * @param abb3PartyTmp the ABB3 party instance
     * @param elementBitLen bit length of each element
     * @param sketchSize size of the sketch table
     * @return array of triplet Z2 vectors containing the update data
     */
    private TripletZ2Vector[] genUpdateRowData(Abb3Party abb3PartyTmp, int elementBitLen, int sketchSize) {
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        secureRandom.setSeed(randKeys);
        SecureRandom finalSecureRandom = secureRandom;
        BigInteger[] updateData = dataGenerator.genUpdateData(elementBitLen, sketchSize, dataType, finalSecureRandom);
        return (TripletZ2Vector[]) abb3PartyTmp.getZ2cParty().setPublicValues(IntStream.range(0, sketchSize)
            .mapToObj(i -> BitVectorFactory.create(elementBitLen, updateData[i]))
            .toArray(BitVector[]::new));
    }


    /**
     * Constructs a GKMain instance with the given properties and party name.
     * <p>
     * Reads experiment configuration parameters including sketch sizes, key bit lengths,
     * payload bit lengths, update sizes, epsilon values, and query frequencies.
     * </p>
     *
     * @param properties configuration properties
     * @param ownName name of the current party
     */
    public GKMain(Properties properties, String ownName) {
        super(properties, ownName);
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes = PropertiesUtils.readLogIntArray(properties, "log_sketch_size");
        keyBitLen = PropertiesUtils.readLogIntArray(properties, "key_bit_len");
        payloadBitLen = PropertiesUtils.readLogIntArray(properties, "payload_bit_len");
        logUpdateSizes = PropertiesUtils.readLogIntArray(properties, "log_update_size");
        epsilons = PropertiesUtils.readDoubleArray(properties, "epsilons");
        queryFrequencies = PropertiesUtils.readIntArray(properties, "query_frequency");
        LOGGER.info("{} read gk config", ownRpc.ownParty().getPartyName());
        config = GKConfigUtils.createConfig(properties);
    }

    /**
     * Runs the GK experiment for the specified party.
     * <p>
     * Orchestrates the complete experiment workflow including output file creation,
     * warmup, and execution of all configured experiments with performance measurement.
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @throws IOException if file I/O error occurs
     * @throws MpcAbortException if MPC protocol error occurs
     */
    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());

        // Create output file with descriptive name
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        // Write header with performance metrics
        String tab = "Party ID\tLog Sketch Size\tKey Bit Len\tPayload Bit Len\tLog Update Size"
            + "\tParallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        // Warmup phase
        warmup(ownRpc, taskId);
        taskId++;

        // Run all configured experiments
        for (int i = 0; i < logSketchSizes.length; i++) {
            runOneTest(parallel, ownRpc, taskId, logSketchSizes[i], keyBitLen[i], payloadBitLen[i], logUpdateSizes[i], epsilons[i], queryFrequencies[i], printWriter);
            taskId++;
        }

        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    /**
     * Performs warmup to ensure stable performance measurements.
     * <p>
     * Runs a small-scale experiment to trigger JIT compilation and eliminate
     * cold-start effects before actual measurements begin.
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @param taskId the task identifier for warmup
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        GKTable table = initData(ownRpc, WARMUP_LOG_SKETCH_SIZE, WARMUP_KEY_BIT_LEN, WARMUP_PAYLOAD_BIT_LEN, WARMUP_EPSILON);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GKParty gkGroup = GKFactory.createParty(abb3Party, config);
        gkGroup.setTaskId(taskId);
        gkGroup.setParallel(false);
        gkGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} init", gkGroup.ownParty().getPartyName());
        gkGroup.init();
        gkGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} execute", gkGroup.ownParty().getPartyName());
        runOp(gkGroup, table, WARMUP_UPDATE_NUM, WARMUP_QUERY, null);
        gkGroup.getRpc().synchronize();
        gkGroup.getRpc().reset();
        LOGGER.info("(warmup) {} finish", gkGroup.ownParty().getPartyName());
    }

    /**
     * Runs a single experiment with the specified parameters.
     * <p>
     * Performs one complete test including initialization, protocol execution,
     * and performance measurement with timing and communication statistics.
     * </p>
     *
     * @param parallel whether to use parallel execution
     * @param ownRpc the RPC instance for the current party
     * @param taskId the task identifier
     * @param logSketchSize logarithm of sketch table size
     * @param keyBitLen bit length of keys
     * @param payloadBitLen bit length of payload
     * @param logUpdateSize logarithm of update count
     * @param epsilon error tolerance parameter
     * @param queryFrequency query frequency
     * @param printWriter writer for outputting results
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int logSketchSize, int keyBitLen, int payloadBitLen, int logUpdateSize
        , double epsilon, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: tableSize = {}, keyDim = {}, payloadDim = {}, logUpdateNum = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), 1 << logSketchSize, keyBitLen, payloadBitLen, logUpdateSize, parallel
        );
        GKTable table = initData(ownRpc, logSketchSize, keyBitLen, payloadBitLen, epsilon);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        GKParty gkGroup = GKFactory.createParty(abb3Party, config);
        gkGroup.setTaskId(taskId);
        gkGroup.setParallel(parallel);

        gkGroup.getRpc().synchronize();
        gkGroup.getRpc().reset();

        // Measure initialization phase
        LOGGER.info("{} init", gkGroup.ownParty().getPartyName());
        stopWatch.start();
        gkGroup.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = gkGroup.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = gkGroup.getRpc().getPayloadByteLength();
        long initSendByteLength = gkGroup.getRpc().getSendByteLength();
        gkGroup.getRpc().synchronize();
        gkGroup.getRpc().reset();

        // Measure protocol execution phase
        LOGGER.info("{} execute", gkGroup.ownParty().getPartyName());
        runOp(gkGroup, table, 1 << logUpdateSize, queryFrequency, printWriter);
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = gkGroup.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = gkGroup.getRpc().getPayloadByteLength();
        long ptoSendByteLength = gkGroup.getRpc().getSendByteLength();
        String info = gkGroup.ownParty().getPartyId()
            + "\t" + logSketchSize + "\t" + keyBitLen + "\t" + payloadBitLen + "\t" + logUpdateSize
            + "\t" + parallel + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);

        gkGroup.getRpc().synchronize();
        gkGroup.getRpc().reset();
        LOGGER.info("{} finish", gkGroup.ownParty().getPartyName());
    }

    /**
     * Executes the update and query operations for the GK protocol.
     * <p>
     * Performs a stream of updates with periodic queries, regenerating update data
     * when needed and logging progress at specified intervals.
     * </p>
     *
     * @param gkParty the GK party instance
     * @param table the GK table to operate on
     * @param updateNum total number of updates to perform
     * @param queryFrequency frequency of queries
     * @param printWriter writer for outputting progress
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOp(GKParty gkParty, GKTable table, int updateNum, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        TripletZ2Vector[] queryData = genUpdateRowData(gkParty.getAbb3Party(), table.getKeyBitLen(), 1);
        queryData = gkParty.getAbb3Party().getZ2cParty().matrixTranspose((Arrays.stream(queryData).map(ea -> (TripletZ2Vector) ea)).toArray(TripletZ2Vector[]::new));
        int cur = table.getTableSize();
        stopWatch.start();
        int next = table.getTableSize();
        for (int i = 0; i < updateNum; i++) {
            // Regenerate update data when we've used all elements in current batch
            stopWatch.suspend();
            if (i % cur == 0) {
                updateData = genUpdateRowData(gkParty.getAbb3Party(), table.getKeyBitLen(), table.getTableSize());
            }
            stopWatch.resume();
            // Perform periodic queries
            if (i % queryFrequency == 0) {
                LOGGER.info("updating index: {} / {}", i, updateNum);
                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
                LOGGER.info("ptoPayloadByteLength: {}", gkParty.getRpc().getPayloadByteLength());
                LOGGER.info("ptoSendByteLength: {}", gkParty.getRpc().getSendByteLength());
                String info = String.format("query index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                    i, updateNum, stopWatch.getTime(TimeUnit.MILLISECONDS), gkParty.getRpc().getPayloadByteLength(), gkParty.getRpc().getSendByteLength());
                if (printWriter != null) {
                    printWriter.println(info);
                }
                LOGGER.info("query index: {} / {}", i, updateNum);
                gkParty.getQuery(table, queryData);
            }
            // Track progress for data regeneration
            if (i == next) {
                next += table.getTableSize();
            }
            MpcZ2Vector[] toUpdate = new MpcZ2Vector[]{updateData[i % cur]};
            gkParty.update(table, toUpdate);
        }
        String info = String.format("final update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
            stopWatch.getTime(TimeUnit.MILLISECONDS), gkParty.getRpc().getPayloadByteLength(), gkParty.getRpc().getSendByteLength());
        if (printWriter != null) {
            printWriter.println(info);
        }
        stopWatch.reset();
        gkParty.getAbb3Party().checkUnverified();
    }

    /**
     * Initializes a GK table with the specified parameters.
     * <p>
     * Creates and initializes a GK sketch table with random shared data
     * and generates a shared random seed for deterministic data generation.
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @param logSketchSize logarithm of sketch table size
     * @param keyBitLen bit length of keys
     * @param payloadBitLen bit length of payload
     * @param epsilon error tolerance parameter
     * @return initialized GK table
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private GKTable initData(Rpc ownRpc, int logSketchSize, int keyBitLen, int payloadBitLen, double epsilon) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // Generate initial random shared data for the sketch
        TripletZ2Vector[] initData = IntStream.range(0, keyBitLen + payloadBitLen)
            .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1 << logSketchSize))
            .toArray(TripletZ2Vector[]::new);
        GKTable gkTable = new GKTable(initData, 1 << logSketchSize, keyBitLen, payloadBitLen, epsilon);
        // Generate shared random key for deterministic data generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return gkTable;
    }
}

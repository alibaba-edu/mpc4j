package edu.alibaba.mpc4j.work.db.sketch.main.HLL;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpParty;
import edu.alibaba.mpc4j.s3pc.abb3.mainpto.AbstractMainAbb3PartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.db.sketch.HLL.*;
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
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * HyperLogLog (HLL) sketch experiment main class.
 * <p>
 * This class implements the experiment runner for the HLL sketch protocol in a 3PC environment.
 * HLL is a probabilistic data structure for estimating the cardinality of sets (counting distinct elements).
 * The experiment measures performance metrics including initialization time, protocol execution time,
 * communication overhead, and supports both parallel and sequential execution.
 * </p>
 */
public class HLLMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(HLLMain.class);

    /**
     * Protocol type name identifier
     */
    public static final String PTO_TYPE = "HLL";
    /**
     * Protocol type name for Z2 implementation
     */
    public static final String PTO_TYPE_NAME = "HLL_Z2";
    /**
     * Configuration key for protocol type
     */
    public static final String PTO_NAME_KEY = "hll_pto_name";

    // Warmup configuration constants
    /**
     * Logarithm of sketch table size for warmup phase
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 8;
    /**
     * Hash bit length for warmup phase
     */
    private static final int WARMUP_HASH_BIT_LEN = 20;
    /**
     * Logarithm of update count for warmup phase
     */
    private static final int WARMUP_LOG_UPDATE_NUM = 10;
    /**
     * Element bit length for warmup phase
     */
    private static final int WARMUP_ELEMENT_BIT_LEN = 32;
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
     * Hash bit lengths for each experiment
     */
    private final int[] hashBitLens;
    /**
     * Element bit lengths for each experiment
     */
    private final int[] elementBitLens;
    /**
     * Logarithm of update data sizes for each experiment
     */
    private final int[] logUpdateSizes;
    /**
     * Query frequencies for each experiment
     */
    private final int[] queryFrequencies;
    /**
     * HLL protocol configuration
     */
    private final HLLConfig config;
    /**
     * Random seed keys for deterministic data generation
     */
    private byte[] randKeys;

    private final DataGenerator dataGenerator = new DataGenerator();
    /**
     * Type of data distribution for experiments
     */
    private String dataType = "UNIFORM";

    /**
     * Generates update row data as shared Z2 vectors.
     * <p>
     * Creates public values for all elements in the sketch table using a shared
     * random seed to ensure all parties have the same data.
     * </p>
     *
     * @param abb3PartyTmp  the ABB3 party instance
     * @param elementBitLen bit length of each element
     * @param sketchSize    size of the sketch table
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
     * Constructs an HLLMain instance with the given properties and party name.
     * <p>
     * Reads experiment configuration parameters including sketch sizes, hash bit lengths,
     * element bit lengths, update sizes, and query frequencies.
     * </p>
     *
     * @param properties configuration properties
     * @param ownName    name of the current party
     */
    public HLLMain(Properties properties, String ownName) {
        super(properties, ownName);
        // Read PTO configuration parameters
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes = PropertiesUtils.readLogIntArray(properties, "log_sketch_size");
        hashBitLens = PropertiesUtils.readLogIntArray(properties, "hash_bit_len");
        queryFrequencies = PropertiesUtils.readIntArray(properties, "query_frequency");
        elementBitLens = PropertiesUtils.readLogIntArray(properties, "element_bit_len");
        logUpdateSizes = PropertiesUtils.readLogIntArray(properties, "log_update_size");
        LOGGER.info("{} read hll config", ownRpc.ownParty().getPartyName());
        config = HLLConfigUtils.createConfig(properties);
    }

    /**
     * Runs the HLL experiment for the specified party.
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
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + appendString
                + "_" + ownRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        // Write header with performance metrics
        String tab = "Party ID\tUpdate Size\tIs Parallel\tThread Num"
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
            runOneTest(parallel, ownRpc, taskId, logSketchSizes[i], hashBitLens[i],
                    elementBitLens[i], logUpdateSizes[i], queryFrequencies[i],printWriter);
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
        HLLTable table = initData(ownRpc, WARMUP_ELEMENT_BIT_LEN, WARMUP_LOG_SKETCH_SIZE, WARMUP_HASH_BIT_LEN);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        HLLParty hllGroup = HLLFactory.createHLLParty(abb3Party, config);
        hllGroup.setTaskId(taskId);
        hllGroup.setParallel(false);
        hllGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} init", hllGroup.ownParty().getPartyName());
        hllGroup.init();
        hllGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} execute", hllGroup.ownParty().getPartyName());
        runOp(hllGroup, table, 1 << WARMUP_LOG_UPDATE_NUM, WARMUP_QUERY, null);
        hllGroup.getRpc().synchronize();
        hllGroup.getRpc().reset();
        LOGGER.info("(warmup) {} finish", hllGroup.ownParty().getPartyName());
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
     * @param hashBitLen hash bit length
     * @param elementBitLen element bit length
     * @param logUpdateSize logarithm of update count
     * @param queryFreq query frequency
     * @param printWriter writer for outputting results
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId,
                            int logSketchSize, int hashBitLen, int elementBitLen,
                            int logUpdateSize,int queryFreq, PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
                "{}: logSketchSize = {}, hashBitLen = {}, logUpdateSize ={}, parallel = {}",
                ownRpc.ownParty().getPartyName(), logSketchSize, hashBitLen, logUpdateSize, parallel
        );
        HLLTable table = initData(ownRpc, elementBitLen, logSketchSize, hashBitLen);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        HLLParty hllGroup = HLLFactory.createHLLParty(abb3Party, config);
        hllGroup.setTaskId(taskId);
        hllGroup.setParallel(parallel);

        hllGroup.getRpc().synchronize();
        hllGroup.getRpc().reset();

        // Measure initialization phase
        LOGGER.info("{} init", hllGroup.ownParty().getPartyName());
        stopWatch.start();
        hllGroup.init();
        hllGroup.getRpc().synchronize();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = hllGroup.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = hllGroup.getRpc().getPayloadByteLength();
        long initSendByteLength = hllGroup.getRpc().getSendByteLength();
        hllGroup.getRpc().synchronize();
        hllGroup.getRpc().reset();

        // Measure protocol execution phase
        LOGGER.info("{} execute", hllGroup.ownParty().getPartyName());
        runOp(hllGroup, table, 1 << logUpdateSize, queryFreq, printWriter);
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        long ptoDataPacketNum = hllGroup.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = hllGroup.getRpc().getPayloadByteLength();
        long ptoSendByteLength = hllGroup.getRpc().getSendByteLength();
        String info = hllGroup.ownParty().getPartyId()
                + "\t" + logUpdateSize
                + "\t" + logSketchSize
                + "\t" + hashBitLen
                + "\t" + parallel
                + "\t" + ForkJoinPool.getCommonPoolParallelism()
                + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
                + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);

        hllGroup.getRpc().synchronize();
        hllGroup.getRpc().reset();
        LOGGER.info("{} finish", hllGroup.ownParty().getPartyName());
    }

    /**
     * Executes the update and query operations for the HLL protocol.
     * <p>
     * Performs a stream of updates with periodic queries, regenerating update data
     * when needed and logging progress at specified intervals.
     * </p>
     *
     * @param hllParty the HLL party instance
     * @param hllTable the HLL table to operate on
     * @param updateNum total number of updates to perform
     * @param queryFrequency frequency of queries
     * @param printWriter writer for outputting progress
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOp(HLLParty hllParty, AbstractHLLTable hllTable, int updateNum, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        int initSketchSize = hllTable.getTableSize();
        stopWatch.start();
        for (int i = 0; i < updateNum; i++) {
            // Perform periodic queries to estimate cardinality
            if (i % queryFrequency == 0 ) {
                LOGGER.info("updating index: {} / {}", i, updateNum);
                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
                LOGGER.info("ptoPayloadByteLength: {}", hllParty.getRpc().getPayloadByteLength());
                LOGGER.info("ptoSendByteLength: {}", hllParty.getRpc().getSendByteLength());
                String info = String.format("query index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                        i, updateNum, stopWatch.getTime(TimeUnit.MILLISECONDS), hllParty.getRpc().getPayloadByteLength(), hllParty.getRpc().getSendByteLength());
                if (printWriter != null) {
                    printWriter.println(info);
                }
                LOGGER.info("query index: {} / {}", i, updateNum);
                hllParty.query(hllTable);
            }
            // Regenerate update data when we've used all elements in current batch
            stopWatch.suspend();
            if (i % initSketchSize == 0) {
                updateData = genUpdateRowData(hllParty.getAbb3Party(), hllTable.getElementBitLen(), initSketchSize);
            }
            stopWatch.resume();
            MpcZ2Vector[] toUpdate = new MpcZ2Vector[]{updateData[i % initSketchSize]};
            hllParty.update(hllTable, toUpdate);
        }
        String info = String.format("final update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                stopWatch.getTime(TimeUnit.MILLISECONDS), hllParty.getRpc().getPayloadByteLength(), hllParty.getRpc().getSendByteLength());
        if (printWriter != null) {
            printWriter.println(info);
        }
        stopWatch.reset();
        hllParty.getAbb3Party().checkUnverified();
    }

    /**
     * Initializes an HLL table with the specified parameters.
     * <p>
     * Creates and initializes an HLL sketch table including hash parameters,
     * initial shared data, and generates a shared random seed.
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @param elementBitLen bit length of elements
     * @param logSketchSize logarithm of sketch table size
     * @param hashBitLen hash bit length
     * @return initialized HLL table
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private HLLTable initData(Rpc ownRpc, int elementBitLen, int logSketchSize, int hashBitLen) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // Generate hash parameters for the sketch
        TripletZ2Vector encKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        // Initialize sketch table with zero shares
        int payloadBitLen = LongUtils.ceilLog2(hashBitLen);
        TripletZ2Vector[] initShareData = IntStream.range(0, payloadBitLen)
                .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1 << logSketchSize))
                .toArray(TripletZ2Vector[]::new);
        HLLTable hllTable = new HLLTable(initShareData, hashBitLen, elementBitLen, logSketchSize, encKey);
        // Generate shared random key for deterministic data generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return hllTable;
    }
}

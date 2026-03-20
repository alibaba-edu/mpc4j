package edu.alibaba.mpc4j.work.db.sketch.main.CMS;

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
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.CMS.*;
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
 * Count-Min Sketch (CMS) Z2 implementation experiment main class.
 * <p>
 * This class implements the experiment runner for the CMS sketch protocol using Z2 arithmetic in a 3PC environment.
 * CMS is a probabilistic data structure used for frequency estimation of streaming data.
 * The experiment measures performance metrics including initialization time, protocol execution time,
 * communication overhead (data packets, payload bytes, send bytes), and supports both parallel and sequential execution.
 * </p>
 */
public class CMSZ2Main extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMSZ2Main.class);
    /**
     * Protocol name identifier
     */
    public static final String PTO_NAME = "CMS";
    /**
     * Protocol type name for Z2 implementation
     */
    public static final String PTO_TYPE_NAME = "CMS_Z2";
    /**
     * Configuration key for protocol type
     */
    public static final String PTO_NAME_KEY = "cms_pto_name";

    // Warmup configuration constants to ensure JIT compilation and stable performance
    /**
     * Element bit length for warmup phase
     */
    private static final int WARMUP_ELEMENT_BIT_LEN = 20;
    /**
     * Logarithm of sketch table size for warmup phase
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 10;
    /**
     * Payload bit length for warmup phase
     */
    private static final int WARMUP_PAYLOAD_BIT_LEN = 12;
    /**
     * Logarithm of update count for warmup phase
     */
    private static final int WARMUP_LOG_UPDATE_NUM = 12;
    /**
     * Query frequency for warmup phase
     */
    private static final int WARMUP_QUERY_FRE = 100;

    // Experiment configuration parameters
    /**
     * Bit length of input elements for each experiment
     */
    private final int[] elementBitLens;
    /**
     * Logarithm of sketch table sizes for each experiment
     */
    private final int[] logSketchSizes;
    /**
     * Payload bit lengths for each experiment
     */
    private final int[] payloadBitLens;
    /**
     * Logarithm of update data sizes for each experiment
     */
    private static int[] logUpdateSizes;
    /**
     * Query frequencies for each experiment (how often to perform queries during updates)
     */
    private static int[] queryFres;
    /**
     * CMS protocol configuration
     */
    private final CMSConfig config;
    /**
     * Random seed keys for deterministic data generation across parties
     */
    private byte[] randKeys;

    private final DataGenerator dataGenerator = new DataGenerator();
    /**
     * Type of data distribution for experiments (e.g., UNIFORM, ZIPF)
     */
    private String dataType = "UNIFORM";

    /**
     * Generates update row data as shared Z2 vectors.
     * <p>
     * This method creates public values for all elements in the sketch table, which will be used
     * for update operations. The data is generated deterministically using a shared random seed
     * to ensure all parties have the same data.
     * </p>
     *
     * @param abb3PartyTmp  the ABB3 party instance for creating shared vectors
     * @param elementBitLen bit length of each element
     * @param logSketchSize logarithm of the sketch table size
     * @return array of triplet Z2 vectors containing the update data
     */
    private TripletZ2Vector[] genUpdateRowData(Abb3Party abb3PartyTmp, int elementBitLen, int logSketchSize) {
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        // Use shared random seed for deterministic data generation
        secureRandom.setSeed(randKeys);
        SecureRandom finalSecureRandom = secureRandom;
        BigInteger[] updateData = dataGenerator.genUpdateData(elementBitLen, 1 << logSketchSize, dataType, finalSecureRandom);
        return (TripletZ2Vector[]) abb3PartyTmp.getZ2cParty().setPublicValues(IntStream.range(0, 1 << logSketchSize)
            .mapToObj(i -> BitVectorFactory.create(elementBitLen, updateData[i]))
            .toArray(BitVector[]::new));
    }


    /**
     * Constructs a CMSZ2Main instance with the given properties and party name.
     * <p>
     * Reads experiment configuration parameters including sketch sizes, element bit lengths,
     * payload bit lengths, update sizes, and query frequencies from the properties file.
     * </p>
     *
     * @param properties configuration properties containing experiment parameters
     * @param ownName name of the current party
     */
    public CMSZ2Main(Properties properties, String ownName) {
        super(properties, ownName);
        // Read PTO configuration parameters
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes = PropertiesUtils.readLogIntArray(properties, "log_sketch_size");
        elementBitLens = PropertiesUtils.readLogIntArray(properties, "element_bit_len");
        logUpdateSizes = PropertiesUtils.readLogIntArray(properties, "log_update_size");
        payloadBitLens = PropertiesUtils.readLogIntArray(properties, "payload_bit_len");
        queryFres = PropertiesUtils.readIntArray(properties, "query_frequency");
        LOGGER.info("{} read cms config", ownRpc.ownParty().getPartyName());
        config = CMSConfigUtils.createConfig(properties);
    }

    /**
     * Runs the CMS experiment for the specified party.
     * <p>
     * This method orchestrates the complete experiment workflow:
     * 1. Creates an output file to store performance metrics
     * 2. Connects to other parties
     * 3. Performs warmup to ensure stable performance
     * 4. Runs all configured experiments with different parameters
     * 5. Records timing and communication statistics
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @throws IOException if file I/O error occurs
     * @throws MpcAbortException if MPC protocol error occurs
     */
    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());

        // Create output file with descriptive name including configuration details
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        // Write header with performance metrics columns
        String tab = "Party ID\tElementBitLen\tLogSketchSize\tLogUpdateSize\tPayloadBitLen\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        // Warmup phase to trigger JIT compilation and stabilize performance
        warmup(ownRpc, taskId);
        taskId++;

        // Run all configured experiments
        for (int i = 0; i < logSketchSizes.length; i++) {
            runOneTest(parallel, ownRpc, taskId, elementBitLens[i], logSketchSizes[i], logUpdateSizes[i], payloadBitLens[i], queryFres[i], printWriter);
            taskId++;
        }

        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();

    }

    /**
     * Performs warmup to ensure stable performance measurements.
     * <p>
     * Warmup runs a small-scale experiment to trigger JIT compilation and
     * eliminate cold-start effects before actual measurements begin.
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @param taskId the task identifier for this warmup
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        Z2CMSTable table = initData(ownRpc, WARMUP_ELEMENT_BIT_LEN, WARMUP_LOG_SKETCH_SIZE, WARMUP_PAYLOAD_BIT_LEN);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        CMSParty cmsGroup = CMSFactory.createParty(abb3Party, config);
        cmsGroup.setTaskId(taskId);
        cmsGroup.setParallel(false);
        cmsGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} init", cmsGroup.ownParty().getPartyName());
        cmsGroup.init();
        cmsGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} execute", cmsGroup.ownParty().getPartyName());
        runOp(cmsGroup, table, 1 << WARMUP_LOG_UPDATE_NUM, null, WARMUP_QUERY_FRE);
        cmsGroup.getRpc().synchronize();
        cmsGroup.getRpc().reset();
        LOGGER.info("(warmup) {} finish", cmsGroup.ownParty().getPartyName());
    }

    /**
     * Runs a single experiment with the specified parameters.
     * <p>
     * This method performs one complete test including initialization, protocol execution,
     * and performance measurement. It records timing and communication statistics.
     * </p>
     *
     * @param parallel whether to use parallel execution
     * @param ownRpc the RPC instance for the current party
     * @param taskId the task identifier
     * @param elementBitLen bit length of elements
     * @param logSketchSize logarithm of sketch table size
     * @param logUpdateSize logarithm of update count
     * @param payloadBitLen bit length of payload
     * @param queryFre query frequency
     * @param printWriter writer for outputting results
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId,
                            int elementBitLen, int logSketchSize, int logUpdateSize, int payloadBitLen, int queryFre,
                            PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info("{}: elementBitLen = {}, logSketchSize = {}, logUpdateSize = {}, payloadBitLen = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), elementBitLen, logSketchSize, logUpdateSize, payloadBitLen, parallel
        );
        Z2CMSTable table = initData(ownRpc, elementBitLen, logSketchSize, payloadBitLen);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        CMSParty cmsGroup = CMSFactory.createParty(abb3Party, config);
        cmsGroup.setTaskId(taskId);
        cmsGroup.setParallel(parallel);

        cmsGroup.getRpc().synchronize();
        cmsGroup.getRpc().reset();

        // Measure initialization phase
        LOGGER.info("{} init", cmsGroup.ownParty().getPartyName());
        stopWatch.start();
        cmsGroup.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = cmsGroup.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = cmsGroup.getRpc().getPayloadByteLength();
        long initSendByteLength = cmsGroup.getRpc().getSendByteLength();
        cmsGroup.getRpc().synchronize();
        cmsGroup.getRpc().reset();

        // Measure protocol execution phase
        LOGGER.info("{} execute", cmsGroup.ownParty().getPartyName());
        runOp(cmsGroup, table, 1 << logUpdateSize, printWriter, queryFre);
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = cmsGroup.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = cmsGroup.getRpc().getPayloadByteLength();
        long ptoSendByteLength = cmsGroup.getRpc().getSendByteLength();
        String info = cmsGroup.ownParty().getPartyId()
            + "\t" + elementBitLen + "\t" + logSketchSize + "\t" + logUpdateSize + "\t" + payloadBitLen
            + "\t" + parallel
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);

        cmsGroup.getRpc().synchronize();
        cmsGroup.getRpc().reset();
        LOGGER.info("{} finish", cmsGroup.ownParty().getPartyName());
    }

    /**
     * Executes the update and query operations for the CMS protocol.
     * <p>
     * This method performs a stream of updates with periodic queries to simulate
     * real-world streaming scenarios. It regenerates update data when needed and
     * logs progress and performance metrics at specified intervals.
     * </p>
     *
     * @param cmsParty the CMS party instance
     * @param table the CMS table to operate on
     * @param updateSize total number of updates to perform
     * @param printWriter writer for outputting progress (null for warmup)
     * @param queryFrequency frequency of queries (perform query every N updates)
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private void runOp(CMSParty cmsParty, Z2CMSTable table, int updateSize, PrintWriter printWriter, int queryFrequency) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        TripletZ2Vector[] queryData = genUpdateRowData(cmsParty.getAbb3Party(), table.getElementBitLen(), 1);
        stopWatch.start();
        for (int i = 0; i < updateSize; i++) {
            // Perform periodic queries to simulate real-time monitoring
            if (i % queryFrequency == 0) {
                LOGGER.info("updating index: {} / {}", i, updateSize);
                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
                LOGGER.info("ptoPayloadByteLength: {}", cmsParty.getRpc().getPayloadByteLength());
                LOGGER.info("ptoSendByteLength: {}", cmsParty.getRpc().getSendByteLength());
                String info = String.format("query index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                    i, updateSize, stopWatch.getTime(TimeUnit.MILLISECONDS), cmsParty.getRpc().getPayloadByteLength(), cmsParty.getRpc().getSendByteLength());
                if (printWriter != null) {
                    printWriter.println(info);
                }
                LOGGER.info("query index: {} / {}", i, updateSize);
                cmsParty.getQuery(table, new MpcZ2Vector[]{queryData[0]});
            }
            // Regenerate update data when we've used all elements in current batch
            stopWatch.suspend();
            if (i % table.getTableSize() == 0) {
                updateData = genUpdateRowData(cmsParty.getAbb3Party(), table.getElementBitLen(), table.getLogSketchSize());
            }
            stopWatch.resume();
            MpcZ2Vector[] toUpdate = new MpcZ2Vector[]{updateData[i % table.getTableSize()]};
            cmsParty.update(table, toUpdate);
        }
        String info = String.format("final update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
            stopWatch.getTime(TimeUnit.MILLISECONDS), cmsParty.getRpc().getPayloadByteLength(), cmsParty.getRpc().getSendByteLength());
        if (printWriter != null) {
            printWriter.println(info);
        }
        stopWatch.reset();
        cmsParty.getAbb3Party().checkUnverified();
    }

    /**
     * Initializes a CMS table with the specified parameters.
     * <p>
     * This method creates and initializes a CMS sketch table including:
     * - Hash parameters for the sketch (hash function coefficients and encryption key)
     * - Initial shared data for the sketch table
     * - Random seed for deterministic data generation
     * </p>
     *
     * @param ownRpc the RPC instance for the current party
     * @param elementBitLen bit length of elements
     * @param logSketchSize logarithm of sketch table size
     * @param payloadBitLen bit length of payload
     * @return initialized CMS table
     * @throws MpcAbortException if MPC protocol error occurs
     */
    private Z2CMSTable initData(Rpc ownRpc, int elementBitLen, int logSketchSize, int payloadBitLen) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // Generate hash parameters for the sketch
        TripletZ2Vector encKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        long[] plainAndB = new long[]{0, 0};
        while ((plainAndB[0] == 0 || plainAndB[1] == 0)) {
            TripletLongVector aAndB = abb3PartyTmp.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{2})[0];
            plainAndB = abb3PartyTmp.getLongParty().open(aAndB)[0].getElements();
        }
        HashParameters hashParameters = new HashParameters(plainAndB[0], plainAndB[1], encKey);
        // Initialize sketch table with zero shares
        TripletZ2Vector[] initShareData = IntStream.range(0, payloadBitLen)
            .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1 << logSketchSize))
            .toArray(TripletZ2Vector[]::new);
        Z2CMSTable cmsTable = new Z2CMSTable(initShareData, payloadBitLen, elementBitLen, logSketchSize, hashParameters);
        // Generate shared random key for deterministic data generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return cmsTable;
    }
}

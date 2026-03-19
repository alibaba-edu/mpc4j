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
 * GK main.
 */
public class GKMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(GKMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE = "GK";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "gk_pto_name";
    /**
     * warm up log of sketch table size
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 10;
    /**
     * warm up key bit length
     */
    private static final int WARMUP_KEY_BIT_LEN = 20;
    /**
     * warm up payload bit length
     */
    private static final int WARMUP_PAYLOAD_BIT_LEN = 12;
    /**
     * warm up update number
     */
    private static final int WARMUP_UPDATE_NUM = 1 << 12;
    /**
     * warm up update number
     */
    private static final double WARMUP_EPSILON = 0.1;
    /**
     * warm up update number
     */
    private static final int WARMUP_QUERY = 100;
    /**
     * log of sketch table size
     */
    private final int[] logSketchSizes;
    /**
     * log of update data size
     */
    private final int[] logUpdateSizes;
    /**
     * key bit length
     */
    private final int[] keyBitLen;
    /**
     * payload bit length
     */
    private final int[] payloadBitLen;
    /**
     * payload bit length
     */
    private final double[] epsilons;

    private final int[] queryFrequencies;
    /**
     * config
     */
    private final GKConfig config;
    /**
     * rand keys
     */
    private byte[] randKeys;


    private final DataGenerator dataGenerator = new DataGenerator();
    private String dataType = "UNIFORM";

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

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());

        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + ownRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        String tab = "Party ID\tLog Sketch Size\tKey Bit Len\tPayload Bit Len\tLog Update Size"
            + "\tParallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        warmup(ownRpc, taskId);
        taskId++;

        for (int i = 0; i < logSketchSizes.length; i++) {
            runOneTest(parallel, ownRpc, taskId, logSketchSizes[i], keyBitLen[i], payloadBitLen[i], logUpdateSizes[i], epsilons[i], queryFrequencies[i], printWriter);
            taskId++;
        }

        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

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

        LOGGER.info("{} execute", gkGroup.ownParty().getPartyName());
//        stopWatch.start();
        runOp(gkGroup, table, 1 << logUpdateSize, queryFrequency, printWriter);
//        stopWatch.stop();
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

    private void runOp(GKParty gkParty, GKTable table, int updateNum, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        TripletZ2Vector[] queryData = genUpdateRowData(gkParty.getAbb3Party(), table.getKeyBitLen(), 1);
        queryData = gkParty.getAbb3Party().getZ2cParty().matrixTranspose((Arrays.stream(queryData).map(ea -> (TripletZ2Vector) ea)).toArray(TripletZ2Vector[]::new));
        int cur = table.getTableSize();
        stopWatch.start();
        int next = table.getTableSize();
        for (int i = 0; i < updateNum; i++) {
//
            stopWatch.suspend();
            if (i % cur == 0) {
                updateData = genUpdateRowData(gkParty.getAbb3Party(), table.getKeyBitLen(), table.getTableSize());
//                LOGGER.info("table size: {}", table.getTableSize());
            }
            stopWatch.resume();
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
            if (i == next) {
//                LOGGER.info("updating index: {} / {}", i, updateNum);
//                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
//                LOGGER.info("ptoPayloadByteLength: {}", gkParty.getRpc().getPayloadByteLength());
//                LOGGER.info("ptoSendByteLength: {}", gkParty.getRpc().getSendByteLength());
//                String info = String.format("updating index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
//                        i, updateNum, stopWatch.getTime(TimeUnit.MILLISECONDS), gkParty.getRpc().getPayloadByteLength(), gkParty.getRpc().getSendByteLength());
//                if (printWriter != null) {
//                    printWriter.println(info);
//                }
//                pre = cur;
                next += table.getTableSize();
//                updateData = genUpdateRowData(hllParty.getAbb3Party(), hllTable.getElementBitLen(), initSketchSize);
            }
//            if (i % updateNum == updateNum - 1 ) {
//                LOGGER.info("updating index: {} / {}", i, updateNum);
//                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
//                LOGGER.info("ptoPayloadByteLength: {}", gkParty.getRpc().getPayloadByteLength());
//                LOGGER.info("ptoSendByteLength: {}", gkParty.getRpc().getSendByteLength());
//                String info = String.format("updating index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
//                        i, updateNum, stopWatch.getTime(TimeUnit.MILLISECONDS), gkParty.getRpc().getPayloadByteLength(), gkParty.getRpc().getSendByteLength());
//                if (printWriter != null) {
//                    printWriter.println(info);
//                }
//            }

            MpcZ2Vector[] toUpdate = new MpcZ2Vector[]{updateData[i % cur]};
//            LOGGER.info("updating index: {} / {}", i % table.getTableSize(), updateNum);
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

    private GKTable initData(Rpc ownRpc, int logSketchSize, int keyBitLen, int payloadBitLen, double epsilon) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // generate init data
        TripletZ2Vector[] initData = IntStream.range(0, keyBitLen + 5 * payloadBitLen + 1)
            .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareRandom(1 << logSketchSize))
            .toArray(TripletZ2Vector[]::new);
        GKTable gkTable = new GKTable(initData, 1 << logSketchSize, keyBitLen, payloadBitLen, epsilon);
        // get key for random generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return gkTable;
    }
}

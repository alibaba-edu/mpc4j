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

public class HLLMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(HLLMain.class);

    /**
     * protocol type name
     */
    public static final String PTO_TYPE = "HLL";
    /**
     * protocol type key.
     */
    public static final String PTO_TYPE_NAME="HLL_V1";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY="hll_pto_name";
    /**
     * sketch log table size for warm up
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 8;
    /**
     * log payload for warm up
     */
    private static final int WARMUP_HASH_BIT_LEN = 20;
    /**
     * update log data size for warm up
     */
    private static final int WARMUP_LOG_UPDATE_NUM = 10;

    private static final int WARMUP_ELEMENT_BIT_LEN=32;

    private static final int WARMUP_QUERY = 100;
    /**
     * log of table size
     */
    private final int[] logSketchSizes;
    /**
     * log of value field
     */
    private final int[] hashBitLens;
    private final int[] elementBitLens;
    private final int[] logUpdateSizes;
    private final int[] queryFrequencies;
    /**
     * config
     */
    private final HLLConfig config;
    /**
     * rand keys
     */
    private byte[] randKeys;

    private final DataGenerator dataGenerator=new DataGenerator();
    private String dataType="UNIFORM";

    private TripletZ2Vector[] genUpdateRowData(Abb3Party abb3PartyTmp, int elementBitLen, int sketchSize) {
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        secureRandom.setSeed(randKeys);
        SecureRandom finalSecureRandom = secureRandom;
        BigInteger[] updateData=dataGenerator.genUpdateData(elementBitLen,sketchSize,dataType,finalSecureRandom);
        return (TripletZ2Vector[]) abb3PartyTmp.getZ2cParty().setPublicValues(IntStream.range(0, sketchSize)
                .mapToObj(i -> BitVectorFactory.create(elementBitLen, updateData[i]))
                .toArray(BitVector[]::new));
    }


    public HLLMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes =PropertiesUtils.readLogIntArray(properties,"log_sketch_size");
        hashBitLens =PropertiesUtils.readLogIntArray(properties,"hash_bit_len");
        queryFrequencies = PropertiesUtils.readIntArray(properties, "query_frequency");
        elementBitLens=PropertiesUtils.readLogIntArray(properties, "element_bit_len");
        logUpdateSizes =PropertiesUtils.readLogIntArray(properties, "log_update_size");
        LOGGER.info("{} read hll config", ownRpc.ownParty().getPartyName());
        config = HLLConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty(Rpc ownRpc) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", ownRpc.ownParty().getPartyName());

        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + appendString
                + "_" + ownRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);

        String tab = "Party ID\tUpdate Size\tIs Parallel\tThread Num"
                + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
                + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        warmup(ownRpc, taskId);
        taskId++;

        for (int i = 0; i < logSketchSizes.length; i++) {
            runOneTest(parallel, ownRpc, taskId, logSketchSizes[i], hashBitLens[i],
                    elementBitLens[i], logUpdateSizes[i], queryFrequencies[i],printWriter);
            taskId++;
        }


        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();

    }

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

        LOGGER.info("{} execute", hllGroup.ownParty().getPartyName());
//        stopWatch.start();
        runOp(hllGroup, table, 1 << logUpdateSize, queryFreq, printWriter);
//        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
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

    private void runOp(HLLParty hllParty, AbstractHLLTable hllTable, int updateNum, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        int initSketchSize = hllTable.getTableSize();
        stopWatch.start();
        for (int i = 0; i < updateNum; i++) {
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

    private HLLTable initData(Rpc ownRpc, int elementBitLen, int logSketchSize, int hashBitLen) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // hash parameters
        TripletZ2Vector encKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        // sketch
        int payloadBitLen = LongUtils.ceilLog2(hashBitLen);
        TripletZ2Vector[] initShareData = IntStream.range(0, payloadBitLen)
                .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1 << logSketchSize))
                .toArray(TripletZ2Vector[]::new);
        HLLTable hllTable = new HLLTable(initShareData, hashBitLen, elementBitLen, logSketchSize, encKey);
        // get key for random generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return hllTable;
    }
}

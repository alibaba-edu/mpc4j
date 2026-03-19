package edu.alibaba.mpc4j.work.db.sketch.main.SS;

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
import edu.alibaba.mpc4j.work.db.sketch.SS.SSConfig;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSFactory;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSParty;
import edu.alibaba.mpc4j.work.db.sketch.SS.SSTable;
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
 * MG main.
 */
public class SSMain extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSMain.class);
    /**
     * secure random
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * protocol type name
     */
    public static final String PTO_TYPE = "MG";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "mg_pto_name";
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
    private static final int WARMUP_UPDATE_NUM = 1<<12;

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

    private final int[] queryFrequencies;
    /**
     * config
     */
    private final SSConfig config;
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


    public SSMain(Properties properties, String ownName) {
        super(properties, ownName);
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes = PropertiesUtils.readIntArray(properties, "log_sketch_size");
        keyBitLen = PropertiesUtils.readLogIntArray(properties, "key_bit_len");
        payloadBitLen = PropertiesUtils.readLogIntArray(properties, "payload_bit_len");
        logUpdateSizes = PropertiesUtils.readIntArray(properties, "log_update_size");
        queryFrequencies = PropertiesUtils.readIntArray(properties, "query_frequency");
        LOGGER.info("{} read mg config", ownRpc.ownParty().getPartyName());
        config = SSConfigUtils.createConfig(properties);
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
            runOneTest(parallel, ownRpc, taskId, logSketchSizes[i], keyBitLen[i], payloadBitLen[i], logUpdateSizes[i], queryFrequencies[i], printWriter);
            taskId++;
        }

        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void warmup(Rpc ownRpc, int taskId) throws MpcAbortException {
        SSTable table = inputGen(ownRpc, WARMUP_LOG_SKETCH_SIZE, WARMUP_KEY_BIT_LEN, WARMUP_PAYLOAD_BIT_LEN);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        SSParty mgGroup = SSFactory.createParty(abb3Party, config);
        mgGroup.setTaskId(taskId);
        mgGroup.setParallel(false);
        mgGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} init", mgGroup.ownParty().getPartyName());
        mgGroup.init();
        mgGroup.getRpc().synchronize();

        LOGGER.info("(warmup) {} execute", mgGroup.ownParty().getPartyName());
        runOp(mgGroup, table,  WARMUP_UPDATE_NUM, WARMUP_QUERY, null);
        mgGroup.getRpc().synchronize();
        mgGroup.getRpc().reset();
        LOGGER.info("(warmup) {} finish", mgGroup.ownParty().getPartyName());
    }

    private void runOneTest(boolean parallel, Rpc ownRpc, int taskId, int logSketchSize, int keyBitLen, int payloadBitLen, int logUpdateSize, int queryFreq, PrintWriter printWriter) throws MpcAbortException {
        LOGGER.info(
            "{}: tableSize = {}, keyDim = {}, payloadDim = {}, logUpdateNum = {}, parallel = {}",
            ownRpc.ownParty().getPartyName(), 1 << logSketchSize, keyBitLen, payloadBitLen, logUpdateSize, parallel
        );
        SSTable table = inputGen(ownRpc, logSketchSize, keyBitLen, payloadBitLen);
        Abb3Party abb3Party = new Abb3RpParty(ownRpc, abb3RpConfig);
        SSParty mgGroup = SSFactory.createParty(abb3Party, config);
        mgGroup.setTaskId(taskId);
        mgGroup.setParallel(parallel);

        mgGroup.getRpc().synchronize();
        mgGroup.getRpc().reset();

        LOGGER.info("{} init", mgGroup.ownParty().getPartyName());
        stopWatch.start();
        mgGroup.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = mgGroup.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = mgGroup.getRpc().getPayloadByteLength();
        long initSendByteLength = mgGroup.getRpc().getSendByteLength();
        mgGroup.getRpc().synchronize();
        mgGroup.getRpc().reset();

        LOGGER.info("{} execute", mgGroup.ownParty().getPartyName());
//        stopWatch.start();
        runOp(mgGroup, table, logUpdateSize, queryFreq, printWriter);
//        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = mgGroup.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = mgGroup.getRpc().getPayloadByteLength();
        long ptoSendByteLength = mgGroup.getRpc().getSendByteLength();
        String info = mgGroup.ownParty().getPartyId()
            + "\t" + logSketchSize + "\t" + keyBitLen + "\t" + payloadBitLen + "\t" + logUpdateSize
            + "\t" + parallel + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);

        mgGroup.getRpc().synchronize();
        mgGroup.getRpc().reset();
        LOGGER.info("{} finish", mgGroup.ownParty().getPartyName());
    }

    private void runOp(SSParty SSParty, SSTable table, int updateNum, int queryFrequency, PrintWriter printWriter) throws MpcAbortException {

        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        int initSketchSize = table.getTableSize();
        stopWatch.start();
        for (int i = 0; i < updateNum; i++) {
            if (i % queryFrequency == 0 ) {
                LOGGER.info("updating index: {} / {}", i, updateNum);
                LOGGER.info("Total update time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
                LOGGER.info("ptoPayloadByteLength: {}", SSParty.getRpc().getPayloadByteLength());
                LOGGER.info("ptoSendByteLength: {}", SSParty.getRpc().getSendByteLength());
                String info = String.format("query index: %d / %d, Total update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                        i, updateNum, stopWatch.getTime(TimeUnit.MILLISECONDS), SSParty.getRpc().getPayloadByteLength(), SSParty.getRpc().getSendByteLength());
                if (printWriter != null) {
                    printWriter.println(info);
                }
                LOGGER.info("query index: {} / {}", i, updateNum);
//                SSParty.getQuery(table, 100);
            }
            stopWatch.suspend();
            if (i % initSketchSize == 0) {
                updateData = genUpdateRowData(SSParty.getAbb3Party(), table.getKeyBitLen(), initSketchSize);
            }
            stopWatch.resume();
            MpcZ2Vector[] toUpdate = new MpcZ2Vector[]{updateData[i % initSketchSize]};
            SSParty.update(table, toUpdate);
        }
        String info = String.format("final update time: %d, ptoPayloadByteLength: %d, ptoSendByteLength: %d",
                stopWatch.getTime(TimeUnit.MILLISECONDS), SSParty.getRpc().getPayloadByteLength(), SSParty.getRpc().getSendByteLength());
        if (printWriter != null) {
            printWriter.println(info);
        }
        stopWatch.reset();
        SSParty.getAbb3Party().checkUnverified();
    }

    private SSTable inputGen(Rpc ownRpc, int logSketchSize, int keyBitLen, int payloadBitLen) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // generate init data
        TripletZ2Vector[] initData = IntStream.range(0, keyBitLen + payloadBitLen)
            .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1<<logSketchSize))
            .toArray(TripletZ2Vector[]::new);
        SSTable SSTable = new SSTable(initData, logSketchSize, keyBitLen, payloadBitLen);
        // get key for random generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return SSTable;
    }
}

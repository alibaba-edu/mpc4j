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
 * CMSZ2 main.
 */
public class CMSZ2Main extends AbstractMainAbb3PartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMSZ2Main.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME = "CMS";
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "CMS_V2";
    /**
     * protocol name key.
     */
    public static final String PTO_NAME_KEY = "cms_pto_name";
    /**
     * element bit length for warm up
     */
    private static final int WARMUP_ELEMENT_BIT_LEN = 20;
    /**
     * log of sketch table size for warm up
     */
    private static final int WARMUP_LOG_SKETCH_SIZE = 10;
    /**
     * payload bit length for warm up
     */
    private static final int WARMUP_PAYLOAD_BIT_LEN = 12;
    /**
     * update num for warm up
     */
    private static final int WARMUP_LOG_UPDATE_NUM = 12;
    /**
     * query frequency for warm up
     */
    private static final int WARMUP_QUERY_FRE = 100;
    /**
     * input element bit length
     */
    private final int[] elementBitLens;
    /**
     * log sketch table sizes
     */
    private final int[] logSketchSizes;
    /**
     * payload bit lengths
     */
    private final int[] payloadBitLens;
    /**
     * log update data size
     */
    private static int[] logUpdateSizes;
    /**
     * query frequency
     */
    private static int[] queryFres;
    /**
     * config
     */
    private final CMSConfig config;
    /**
     * rand keys
     */
    private byte[] randKeys;


    private final DataGenerator dataGenerator = new DataGenerator();
    private String dataType = "UNIFORM";

    private TripletZ2Vector[] genUpdateRowData(Abb3Party abb3PartyTmp, int elementBitLen, int logSketchSize) {
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        secureRandom.setSeed(randKeys);
        SecureRandom finalSecureRandom = secureRandom;
        BigInteger[] updateData = dataGenerator.genUpdateData(elementBitLen, 1 << logSketchSize, dataType, finalSecureRandom);
        return (TripletZ2Vector[]) abb3PartyTmp.getZ2cParty().setPublicValues(IntStream.range(0, 1 << logSketchSize)
            .mapToObj(i -> BitVectorFactory.create(elementBitLen, updateData[i]))
            .toArray(BitVector[]::new));
    }


    public CMSZ2Main(Properties properties, String ownName) {
        super(properties, ownName);
        // read PTO config
        LOGGER.info("{} read settings", ownRpc.ownParty().getPartyName());
        logSketchSizes = PropertiesUtils.readLogIntArray(properties, "log_sketch_size");
        elementBitLens = PropertiesUtils.readLogIntArray(properties, "element_bit_len");
        logUpdateSizes = PropertiesUtils.readLogIntArray(properties, "log_update_size");
        payloadBitLens = PropertiesUtils.readLogIntArray(properties, "payload_bit_len");
        queryFres = PropertiesUtils.readIntArray(properties, "query_frequency");
        LOGGER.info("{} read cms config", ownRpc.ownParty().getPartyName());
        config = CMSConfigUtils.createConfig(properties);
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

        String tab = "Party ID\tElementBitLen\tLogSketchSize\tLogUpdateSize\tPayloadBitLen\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);

        ownRpc.connect();
        LOGGER.info("{} ready to run", ownRpc.ownParty().getPartyName());

        int taskId = 0;

        warmup(ownRpc, taskId);
        taskId++;

        for (int i = 0; i < logSketchSizes.length; i++) {
            runOneTest(parallel, ownRpc, taskId, elementBitLens[i], logSketchSizes[i], logUpdateSizes[i], payloadBitLens[i], queryFres[i], printWriter);
            taskId++;
        }


        ownRpc.disconnect();
        printWriter.close();
        fileWriter.close();

    }

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

        LOGGER.info("{} execute", cmsGroup.ownParty().getPartyName());
//        stopWatch.start();
        runOp(cmsGroup, table, 1 << logUpdateSize, printWriter, queryFre);
//        stopWatch.stop();
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

    private void runOp(CMSParty cmsParty, Z2CMSTable table, int updateSize, PrintWriter printWriter, int queryFrequency) throws MpcAbortException {
        TripletZ2Vector[] updateData = new TripletZ2Vector[0];
        TripletZ2Vector[] queryData = genUpdateRowData(cmsParty.getAbb3Party(), table.getElementBitLen(), 1);
//        int fre = 100;
        stopWatch.start();
        for (int i = 0; i < updateSize; i++) {
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

    private Z2CMSTable initData(Rpc ownRpc, int elementBitLen, int logSketchSize, int payloadBitLen) throws MpcAbortException {
        Abb3Party abb3PartyTmp = new Abb3RpParty(ownRpc, abb3RpConfig);
        abb3PartyTmp.init();
        // hash parameters
        TripletZ2Vector encKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        long[] plainAndB = new long[]{0, 0};
        while ((plainAndB[0] == 0 || plainAndB[1] == 0)) {
            TripletLongVector aAndB = abb3PartyTmp.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{2})[0];
            plainAndB = abb3PartyTmp.getLongParty().open(aAndB)[0].getElements();
        }
        HashParameters hashParameters = new HashParameters(plainAndB[0], plainAndB[1], encKey);
        // sketch table
        TripletZ2Vector[] initShareData = IntStream.range(0, payloadBitLen)
            .mapToObj(i -> abb3PartyTmp.getZ2cParty().createShareZeros(1 << logSketchSize))
            .toArray(TripletZ2Vector[]::new);
        Z2CMSTable cmsTable = new Z2CMSTable(initShareData, payloadBitLen, elementBitLen, logSketchSize, hashParameters);
        // get key for random generation
        TripletZ2Vector randKey = (TripletZ2Vector) abb3PartyTmp.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
        randKeys = abb3PartyTmp.getZ2cParty().open(new MpcZ2Vector[]{randKey})[0].getBytes();
        abb3PartyTmp.destroy();
        return cmsTable;
    }

}

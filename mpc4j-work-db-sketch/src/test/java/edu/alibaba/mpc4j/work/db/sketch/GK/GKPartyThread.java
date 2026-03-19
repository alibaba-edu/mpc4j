package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKParty;
import edu.alibaba.mpc4j.work.db.sketch.GK.GKTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * GK party thread.
 */
public class GKPartyThread extends Thread{
    private static final Logger LOGGER = LoggerFactory.getLogger(GKPartyThread.class);
    /**
     * GK party
     */
    private final GKParty gkParty;
    /**
     * ABB3 party
     */
    private final Abb3Party abb3Party;
    /**
     * log of sketch table size
     */
    private final int logSketchSize;
    /**
     * update data
     */
    private final BigInteger[] updateKeys;
    /**
     * query data
     */
    private final BigInteger[] queryKeys;
    /**
     * payload bit length
     */
    private final int payloadBitLen;
    /**
     * key bit length
     */
    private final int keyBitLen;
    /**
     *  error level
     */
    private final double epsilon;
    /**
     * key, count
     */
    private BigInteger[][] sketchRes;
    /**
     * query res: after postprocessing
     */
    private int[] queryRes;

    public GKPartyThread(GKParty gkParty, int logSketchSize, int keyBitLen, int payloadBitLen, double epsilon, BigInteger[] updateKeys, BigInteger[] queryKeys) {
        this.gkParty = gkParty;
        abb3Party = gkParty.getAbb3Party();
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
        this.epsilon = epsilon;
        this.updateKeys = updateKeys;
        this.queryKeys = queryKeys;
    }

    public BigInteger[][] getSketchRes() {
        return sketchRes;
    }

    public int[] getQueryRes() {
        return queryRes;
    }

    public void run() {
        try {
            gkParty.init();
            int tableSize = 1 << logSketchSize;
            TripletZ2Vector[] shareData = IntStream.range(0, keyBitLen + 5*payloadBitLen+1)
                    .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(tableSize))
                    .toArray(TripletZ2Vector[]::new);
            GKTable gkTable = new GKTable(shareData, tableSize, keyBitLen, payloadBitLen, epsilon);
            // sketch
            for (BigInteger data : updateKeys) {
                TripletZ2Vector[] shareKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(keyBitLen, data)});
                gkParty.update(gkTable, shareKey);
            }
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) gkTable.getSketchTable());
            BigInteger[] sketchKeys = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOf(plainSketchRes, keyBitLen)).getBigIntegerData();
            BigInteger[] sketchG1Counts = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen, keyBitLen + payloadBitLen)).getBigIntegerData();
            BigInteger[] sketchG2Counts = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen + payloadBitLen, keyBitLen + 2*payloadBitLen)).getBigIntegerData();
            BigInteger[] sketchDelta1Counts = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen + 2*payloadBitLen, keyBitLen + 3*payloadBitLen)).getBigIntegerData();
            BigInteger[] sketchDelta2Counts = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen + 3*payloadBitLen, keyBitLen + 4*payloadBitLen)).getBigIntegerData();
            BigInteger[] sketchT = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen + 4*payloadBitLen, keyBitLen + 5*payloadBitLen)).getBigIntegerData();
            BigInteger[] flag = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOfRange(plainSketchRes, keyBitLen + 5*payloadBitLen, keyBitLen + 5*payloadBitLen+1)).getBigIntegerData();
            sketchRes = new BigInteger[][] {sketchKeys, sketchG1Counts, sketchG2Counts, sketchDelta1Counts, sketchDelta2Counts, sketchT, flag};
            // query
            TripletZ2Vector[] secretQueryRes = IntStream.range(0, payloadBitLen)
                    .mapToObj(i -> abb3Party.getZ2cParty().createEmpty(false))
                    .toArray(TripletZ2Vector[]::new);
            for (BigInteger data : queryKeys) {
                BitVector[] columnForm = transBitIntegerIntoColumnBit(data, keyBitLen);
                TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(columnForm);
                TripletZ2Vector[] oneQueryRes = (TripletZ2Vector[]) gkParty.getQuery(gkTable, currentKey);
                for (int i = 0; i < payloadBitLen; i++) {
                    secretQueryRes[i].merge(oneQueryRes[i]);
                }
            }
            // there could be some element remaining in the buffer
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(secretQueryRes);
            BigInteger[] beforeProcess = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(plainQueryRes, payloadBitLen)).getBigIntegerData();
            queryRes = Arrays.stream(beforeProcess).mapToInt(
                    ea -> (int)(ea.intValue()-1D)/2
            ).toArray();

            RpZ2Mtp z2Mtp = abb3Party.getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = abb3Party.getTripletProvider().getZl64MtProvider();
            long usedBitTuple = z2Mtp == null ? 0 : z2Mtp.getAllTupleNum();
            long usedLongTuple = zl64Mtp == null ? 0 : zl64Mtp.getAllTupleNum();
            LOGGER.info(" used bitTupleNum:{} | used longTupleNum:{}",
                    usedBitTuple,  usedLongTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
            throw new RuntimeException("error");
        }
    }

    private BitVector[] transBitIntegerIntoColumnBit(BigInteger data, int payloadBitLen) {
        BitVector row = BitVectorFactory.create(payloadBitLen, data);
        return IntStream.range(0, payloadBitLen)
                .mapToObj(i -> row.get(i) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1))
                .toArray(BitVector[]::new);
    }
}

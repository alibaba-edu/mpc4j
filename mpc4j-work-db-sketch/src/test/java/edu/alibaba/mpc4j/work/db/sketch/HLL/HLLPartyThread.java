package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

public class HLLPartyThread extends Thread {
    private static final Logger LOGGER= LoggerFactory.getLogger(HLLPartyThread.class);
    /**
     * CMS party
     */
    private final HLLParty hllParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * log of sketch table size
     */
    private final int logSketchSize;
    /**
     * payload bit length
     */
    private final int hashBitLen;
    /**
     * element bit length
     */
    private final int elementBitLen;
    /**
     * update keys
     */
    private final BigInteger[] updateKeys;
    /**
     * protocol output
     */
    private long[] sketchRes;
    /**
     * query output
     */
    private long[] queryRes;
    /**
     * hash parameters
     */
    private PlainZ2Vector hashKey;

    PlainZ2Vector getHashKey() {
        return hashKey;
    }

    public HLLPartyThread(HLLParty cmsParty, BigInteger[] updateKeys, int elementBitLen, int logSketchSize, int hashBitLen) {
        this.hllParty = cmsParty;
        abb3Party = cmsParty.getAbb3Party();
        this.updateKeys = updateKeys;
        this.logSketchSize = logSketchSize;
        this.elementBitLen = elementBitLen;
        this.hashBitLen = hashBitLen;
    }

    public long[] getSketchRes() {
        return sketchRes;
    }

    public long getQueryRes() {
        // since the query res and estimator is 1-to-1 as the following
        // E = a*S(2^(sum/S))
        // where S is the sketch size, a is set as fixed (0.79)
        // we calculate the estimator in plaintext
        long exp = queryRes[0];
        int size = 1 << logSketchSize;
        long res = (long) (0.79 * size * Math.pow(2, (double) exp /size));
        return res;
    }

    public void run() {
        try{
            hllParty.init();
            // create hash keys
            TripletZ2Vector encKey = (TripletZ2Vector) abb3Party.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
            BitVector plainKey=abb3Party.getZ2cParty().open(new TripletZ2Vector[]{encKey})[0];
            this.hashKey=PlainZ2Vector.create(plainKey);
            AbstractHLLTable hllTable;
            int payloadBitLen = LongUtils.ceilLog2(hashBitLen);
            TripletZ2Vector[] shareData = IntStream.range(0, payloadBitLen)
                        .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(1 << logSketchSize))
                        .toArray(TripletZ2Vector[]::new);
            hllTable = new HLLTable(shareData, hashBitLen, elementBitLen, logSketchSize, encKey);
            // sketch
            for (BigInteger data : updateKeys) {
                    TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(elementBitLen, data)});
                    hllParty.update(hllTable, currentKey);
                }
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) Arrays.copyOf(hllTable.getSketchTable(),hllTable.getSketchTable().length-1));
            sketchRes = MatrixUtils.transBvIntoAv(plainSketchRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();
            // query
            TripletZ2Vector[] secretQueryRes = hllParty.query(hllTable);
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(secretQueryRes);
            queryRes = MatrixUtils.transBvIntoAv(plainQueryRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();

            RpZ2Mtp z2Mtp = abb3Party.getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = abb3Party.getTripletProvider().getZl64MtProvider();
            long usedBitTuple = z2Mtp == null ? 0 : z2Mtp.getAllTupleNum();
            long usedLongTuple = zl64Mtp == null ? 0 : zl64Mtp.getAllTupleNum();
            LOGGER.info(" used bitTupleNum:{} | used longTupleNum:{}", usedBitTuple, usedLongTuple);
        }   catch (MpcAbortException e) {
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

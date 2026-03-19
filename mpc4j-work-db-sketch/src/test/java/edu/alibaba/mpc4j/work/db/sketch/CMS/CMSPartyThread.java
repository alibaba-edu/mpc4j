package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.CMS.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * CMS party thread.
 */
public class CMSPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMSPartyThread.class);
    /**
     * CMS party
     */
    private final CMSParty cmsParty;
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
    private final int payloadBitLen;
    /**
     * element bit length
     */
    private final int elementBitLen;
    /**
     * update keys
     */
    private final BigInteger[] updateKeys;
    /**
     * update keys
     */
    private final BigInteger[] queryKeys;
    /**
     * update keys
     */
    private final boolean isInputBinary;
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
    private HashParameters hashParameters;

    private PlainZ2Vector hashKey;
    PlainZ2Vector getHashKey() {
        return hashKey;
    }

    public CMSPartyThread(CMSParty cmsParty, BigInteger[] updateKeys, BigInteger[] queryKeys, int elementBitLen, int logSketchSize, int payloadBitLen, boolean isInputBinary) {
        this.cmsParty = cmsParty;
        abb3Party = cmsParty.getAbb3Party();
        this.updateKeys = updateKeys;
        this.queryKeys = queryKeys;
        this.logSketchSize = logSketchSize;
        this.elementBitLen = elementBitLen;
        this.payloadBitLen = payloadBitLen;
        this.isInputBinary = isInputBinary;
    }

    public long[] getSketchRes() {
        return sketchRes;
    }

    public long[] getQueryRes() {
        return queryRes;
    }

    public HashParameters getHashParameters() {
        return hashParameters;
    }

    public void run() {
        try{
            cmsParty.init();
            // create hash keys
            TripletZ2Vector encKey = (TripletZ2Vector) abb3Party.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
            BitVector plainKey= abb3Party.getZ2cParty().open(new TripletZ2Vector[]{encKey})[0];
            this.hashKey=PlainZ2Vector.create(plainKey);
            long[] plainAndB = new long[]{0, 0};
            while ((plainAndB[0] == 0 || plainAndB[1] == 0)) {
                TripletLongVector aAndB = abb3Party.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{2})[0];
                plainAndB = abb3Party.getLongParty().open(aAndB)[0].getElements();
            }
            hashParameters = new HashParameters(plainAndB[0], plainAndB[1], encKey);

            AbstractCMSTable cmsTable;
            if (isInputBinary) {
                TripletZ2Vector[] shareData = IntStream.range(0, payloadBitLen)
                    .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(1 << logSketchSize))
                    .toArray(TripletZ2Vector[]::new);
                cmsTable = new Z2CMSTable(shareData, payloadBitLen, elementBitLen, logSketchSize, hashParameters);
                // sketch
                for (BigInteger data : updateKeys) {
                    TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(elementBitLen, data)});
                    cmsParty.update(cmsTable, currentKey);
                }
                BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) cmsTable.getSketchTable());
                sketchRes = MatrixUtils.transBvIntoAv(plainSketchRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();
                // query
                TripletZ2Vector[] secretQueryRes = IntStream.range(0, payloadBitLen)
                    .mapToObj(i -> abb3Party.getZ2cParty().createEmpty(false))
                    .toArray(TripletZ2Vector[]::new);
                for (BigInteger data : queryKeys) {
//                    BitVector[] columnForm = transBitIntegerIntoColumnBit(data, elementBitLen);
                    TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[] {BitVectorFactory.create(elementBitLen, data)});
                    TripletZ2Vector[] oneQueryRes = (TripletZ2Vector[]) cmsParty.getQuery(cmsTable, currentKey);
                    for (int i = 0; i < payloadBitLen; i++) {
                        secretQueryRes[i].merge(oneQueryRes[i]);
                    }
                }
                BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(secretQueryRes);
                queryRes = MatrixUtils.transBvIntoAv(plainQueryRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();
            } else {
                TripletLongVector shareData = (TripletLongVector) abb3Party.getLongParty().setPublicValue(LongVector.createZeros(1 << logSketchSize));
                cmsTable = new CMSTable(new TripletLongVector[]{shareData}, elementBitLen, logSketchSize, hashParameters);
                // sketch
                for (BigInteger data : updateKeys) {
                    TripletLongVector[] currentKey = new TripletLongVector[]{(TripletLongVector) abb3Party.getLongParty().setPublicValue(LongVector.create(new long[]{data.longValue()}))};
                    cmsParty.update(cmsTable, currentKey);
                }
                sketchRes = abb3Party.getLongParty().open((TripletLongVector[]) cmsTable.getSketchTable())[0].getElements();
                // query
                TripletLongVector secretQueryRes = abb3Party.getLongParty().createZeros(0);
                for (BigInteger data : queryKeys) {
                    TripletLongVector[] currentKey = new TripletLongVector[]{(TripletLongVector) abb3Party.getLongParty().setPublicValue(LongVector.create(new long[]{data.longValue()}))};
                    MpcLongVector[] oneQueryRes = (MpcLongVector[]) cmsParty.getQuery(cmsTable, currentKey);
                    secretQueryRes.merge(oneQueryRes[0]);
                }
                queryRes = abb3Party.getLongParty().open(secretQueryRes)[0].getElements();
            }
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

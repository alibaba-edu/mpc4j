package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * MG party thread.
 */
public class SSPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSPartyThread.class);
    /**
     * MG party
     */
    private final SSParty ssParty;
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
     * payload bit length
     */
    private final int payloadBitLen;
    /**
     * key bit length
     */
    private final int keyBitLen;
    /**
     * top k
     */
    private final int topK;
    /**
     * key, count
     */
    private Pair<BigInteger[], BigInteger[]> sketchRes;
    /**
     * query res: key - count
     */
    private Pair<BigInteger[], BigInteger[]> queryRes;

    public SSPartyThread(SSParty ssParty, int logSketchSize, int keyBitLen, int payloadBitLen, BigInteger[] updateKeys, int topK) {
        this.ssParty = ssParty;
        abb3Party = ssParty.getAbb3Party();
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
        this.updateKeys = updateKeys;
        this.topK = topK;
    }

    public Pair<BigInteger[], BigInteger[]> getSketchRes() {
        return sketchRes;
    }

    public Pair<BigInteger[], BigInteger[]> getQueryRes() {
        return queryRes;
    }

    public void run() {
        try {
            ssParty.init();
            int tableSize = 1 << logSketchSize;
            TripletZ2Vector[] shareData = IntStream.range(0, keyBitLen + payloadBitLen)
                .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(tableSize))
                .toArray(TripletZ2Vector[]::new);
            SSTable SSTable = new SSTable(shareData, logSketchSize, keyBitLen, payloadBitLen);
            // sketch
            for (BigInteger data : updateKeys) {
                TripletZ2Vector[] shareKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(keyBitLen, data)});
                ssParty.update(SSTable, shareKey);
            }
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) SSTable.getSketchTable());
            BigInteger[] sketchKeys = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOf(plainSketchRes, keyBitLen)).getBigIntegerData();
            BigInteger[] sketchCounts = ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(plainSketchRes, keyBitLen, keyBitLen + payloadBitLen)).getBigIntegerData();
            sketchRes = Pair.of(sketchKeys, sketchCounts);
            // query
            TripletZ2Vector[] queryResult = (TripletZ2Vector[]) ssParty.getQuery(SSTable, topK);
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(queryResult);
            BigInteger[] queryKeys = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(plainQueryRes, keyBitLen)).getBigIntegerData();
            BigInteger[] queryCounts = ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(plainQueryRes, keyBitLen, keyBitLen + payloadBitLen)).getBigIntegerData();
            queryRes = Pair.of(queryKeys, queryCounts);

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
}

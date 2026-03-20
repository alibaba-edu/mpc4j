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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Thread wrapper for GK (Greenwald-Khanna) party execution in 3PC.
 * This class encapsulates the GK quantile sketch operations performed by each party.
 */
public class GKPartyThread extends Thread{
    private static final Logger LOGGER = LoggerFactory.getLogger(GKPartyThread.class);
    /**
     * GK party instance handling the sketch operations
     */
    private final GKParty gkParty;
    /**
     * ABB3 party providing the underlying secure computation primitives
     */
    private final Abb3Party abb3Party;
    /**
     * Logarithm of the sketch table size (table size = 2^logSketchSize)
     */
    private final int logSketchSize;
    /**
     * Array of keys to be inserted into the sketch during update phase
     */
    private final BigInteger[] updateKeys;
    /**
     * Array of keys to be queried from the sketch
     */
    private final BigInteger[] queryKeys;
    /**
     * Bit length of the payload (count values stored in the sketch)
     */
    private final int payloadBitLen;
    /**
     * Bit length of the input keys
     */
    private final int keyBitLen;
    /**
     * Error parameter epsilon controlling the accuracy of the quantile sketch
     */
    private final double epsilon;
    /**
     * Sketch result containing keys and associated counts
     */
    private BigInteger[][] sketchRes;
    /**
     * Query result after post-processing (rank estimates)
     */
    private int[] queryRes;

    /**
     * Constructs a GK party thread with the specified parameters
     *
     * @param gkParty       the GK party instance
     * @param logSketchSize log of sketch table size
     * @param keyBitLen     bit length of keys
     * @param payloadBitLen bit length of payload
     * @param epsilon       error parameter
     * @param updateKeys    array of keys to update
     * @param queryKeys     array of keys to query
     */
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

    /**
     * Gets the sketch result
     * @return 2D array containing sketch keys and their associated counts
     */
    public BigInteger[][] getSketchRes() {
        return sketchRes;
    }

    /**
     * Gets the query result
     * @return array of rank estimates for queried keys
     */
    public int[] getQueryRes() {
        return queryRes;
    }

    /**
     * Main execution method for the thread
     * Performs sketch initialization, updates, and queries
     */
    public void run() {
        try {
            // Initialize the GK party
            gkParty.init();
            int tableSize = 1 << logSketchSize;
            // Initialize sketch table with shared zeros
            TripletZ2Vector[] shareData = IntStream.range(0, keyBitLen + 5*payloadBitLen+1)
                    .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(tableSize))
                    .toArray(TripletZ2Vector[]::new);
            GKTable gkTable = new GKTable(shareData, tableSize, keyBitLen, payloadBitLen, epsilon);

            // Sketch phase: update the sketch with all update keys
            for (BigInteger data : updateKeys) {
                TripletZ2Vector[] shareKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(keyBitLen, data)});
                gkParty.update(gkTable, shareKey);
            }
            // Open and parse the sketch result
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

            // Query phase: query the sketch for all query keys
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
            // Open and post-process query results
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(secretQueryRes);
            BigInteger[] beforeProcess = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(plainQueryRes, payloadBitLen)).getBigIntegerData();
            queryRes = Arrays.stream(beforeProcess).mapToInt(
                    ea -> (int)(ea.intValue()-1D) / 2
            ).toArray();

            // Log the number of tuples used for performance measurement
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

    /**
     * Transforms a bit integer into column bit representation
     * @param data the integer to transform
     * @param payloadBitLen the payload bit length
     * @return array of bit vectors representing the data in column form
     */
    private BitVector[] transBitIntegerIntoColumnBit(BigInteger data, int payloadBitLen) {
        BitVector row = BitVectorFactory.create(payloadBitLen, data);
        return IntStream.range(0, payloadBitLen)
                .mapToObj(i -> row.get(i) ? BitVectorFactory.createOnes(1) : BitVectorFactory.createZeros(1))
                .toArray(BitVector[]::new);
    }
}

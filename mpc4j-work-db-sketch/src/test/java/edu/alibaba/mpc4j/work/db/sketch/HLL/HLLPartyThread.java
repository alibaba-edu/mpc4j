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

/**
 * Thread wrapper for HLL (HyperLogLog) party execution in 3PC.
 * This class encapsulates the HLL cardinality estimation sketch operations performed by each party.
 */
public class HLLPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(HLLPartyThread.class);
    /**
     * HLL party instance handling the sketch operations
     */
    private final HLLParty hllParty;
    /**
     * ABB3 party providing the underlying secure computation primitives
     */
    private final Abb3Party abb3Party;
    /**
     * Logarithm of the sketch table size (table size = 2^logSketchSize)
     */
    private final int logSketchSize;
    /**
     * Bit length of the hash function output
     */
    private final int hashBitLen;
    /**
     * Bit length of the input elements
     */
    private final int elementBitLen;
    /**
     * Array of keys to be inserted into the sketch during update phase
     */
    private final BigInteger[] updateKeys;
    /**
     * Result of the sketch operation (the sketch table after all updates)
     */
    private long[] sketchRes;
    /**
     * Intermediate query result before estimator calculation
     */
    private long[] queryRes;
    /**
     * Hash key used for the HLL sketch
     */
    private PlainZ2Vector hashKey;

    /**
     * Gets the hash key used for HLL
     * @return the hash key as PlainZ2Vector
     */
    PlainZ2Vector getHashKey() {
        return hashKey;
    }

    /**
     * Constructs an HLL party thread with the specified parameters
     * @param cmsParty the HLL party instance
     * @param updateKeys array of keys to update
     * @param elementBitLen bit length of elements
     * @param logSketchSize log of sketch table size
     * @param hashBitLen bit length of hash output
     */
    public HLLPartyThread(HLLParty cmsParty, BigInteger[] updateKeys, int elementBitLen, int logSketchSize, int hashBitLen) {
        this.hllParty = cmsParty;
        abb3Party = cmsParty.getAbb3Party();
        this.updateKeys = updateKeys;
        this.logSketchSize = logSketchSize;
        this.elementBitLen = elementBitLen;
        this.hashBitLen = hashBitLen;
    }

    /**
     * Gets the sketch result
     * @return array representing the sketch table
     */
    public long[] getSketchRes() {
        return sketchRes;
    }

    /**
     * Gets the estimated cardinality from the query result
     * The HLL estimator is calculated as: E = a * S * 2^(sum/S)
     * where S is the sketch size and a is a constant (0.79)
     * @return estimated cardinality
     */
    public long getQueryRes() {
        // The query result contains the sum of leading zeros
        // We calculate the HLL estimator in plaintext using the formula:
        // E = a * S * 2^(sum/S), where S is the sketch size and a is set to 0.79
        long exp = queryRes[0];
        int size = 1 << logSketchSize;
        long res = (long) (0.79 * size * Math.pow(2, (double) exp /size));
        return res;
    }

    /**
     * Main execution method for the thread
     * Performs sketch initialization, updates, and queries
     */
    public void run() {
        try{
            // Initialize the HLL party
            hllParty.init();
            // Create hash keys for the sketch
            TripletZ2Vector encKey = (TripletZ2Vector) abb3Party.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
            BitVector plainKey = abb3Party.getZ2cParty().open(new TripletZ2Vector[]{encKey})[0];
            this.hashKey = PlainZ2Vector.create(plainKey);
            // Initialize HLL table with shared zeros
            AbstractHLLTable hllTable;
            int payloadBitLen = LongUtils.ceilLog2(hashBitLen);
            TripletZ2Vector[] shareData = IntStream.range(0, payloadBitLen)
                        .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(1 << logSketchSize))
                        .toArray(TripletZ2Vector[]::new);
            hllTable = new HLLTable(shareData, hashBitLen, elementBitLen, logSketchSize, encKey);
            
            // Sketch phase: update the sketch with all update keys
            for (BigInteger data : updateKeys) {
                    TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(elementBitLen, data)});
                    hllParty.update(hllTable, currentKey);
                }
            // Open and get the sketch result (excluding the last element which is used internally)
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) Arrays.copyOf(hllTable.getSketchTable(), hllTable.getSketchTable().length - 1));
            sketchRes = MatrixUtils.transBvIntoAv(plainSketchRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();
            
            // Query phase: compute the cardinality estimate
            TripletZ2Vector[] secretQueryRes = hllParty.query(hllTable);
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(secretQueryRes);
            queryRes = MatrixUtils.transBvIntoAv(plainQueryRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();

            // Log the number of tuples used for performance measurement
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

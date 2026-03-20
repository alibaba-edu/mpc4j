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
 * Thread wrapper for SS (Space-Saving) party execution in 3PC.
 * This class encapsulates the SS sketch operations for finding top-k frequent items performed by each party.
 */
public class SSPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSPartyThread.class);
    /**
     * SS party instance handling the sketch operations
     */
    private final SSParty ssParty;
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
     * Bit length of the payload (count values stored in the sketch)
     */
    private final int payloadBitLen;
    /**
     * Bit length of the input keys
     */
    private final int keyBitLen;
    /**
     * Number of top-k frequent items to retrieve
     */
    private final int topK;
    /**
     * Sketch result containing keys and their associated counts
     */
    private Pair<BigInteger[], BigInteger[]> sketchRes;
    /**
     * Query result containing top-k keys and their counts
     */
    private Pair<BigInteger[], BigInteger[]> queryRes;

    /**
     * Constructs an SS party thread with the specified parameters
     * @param ssParty the SS party instance
     * @param logSketchSize log of sketch table size
     * @param keyBitLen bit length of keys
     * @param payloadBitLen bit length of payload
     * @param updateKeys array of keys to update
     * @param topK number of top-k items to retrieve
     */
    public SSPartyThread(SSParty ssParty, int logSketchSize, int keyBitLen, int payloadBitLen, BigInteger[] updateKeys, int topK) {
        this.ssParty = ssParty;
        abb3Party = ssParty.getAbb3Party();
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
        this.updateKeys = updateKeys;
        this.topK = topK;
    }

    /**
     * Gets the sketch result
     * @return pair containing keys and their associated counts
     */
    public Pair<BigInteger[], BigInteger[]> getSketchRes() {
        return sketchRes;
    }

    /**
     * Gets the query result
     * @return pair containing top-k keys and their counts
     */
    public Pair<BigInteger[], BigInteger[]> getQueryRes() {
        return queryRes;
    }

    /**
     * Main execution method for the thread
     * Performs sketch initialization, updates, and top-k queries
     */
    public void run() {
        try {
            // Initialize the SS party
            ssParty.init();
            int tableSize = 1 << logSketchSize;
            // Initialize sketch table with shared zeros
            TripletZ2Vector[] shareData = IntStream.range(0, keyBitLen + payloadBitLen)
                .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(tableSize))
                .toArray(TripletZ2Vector[]::new);
            SSTable SSTable = new SSTable(shareData, logSketchSize, keyBitLen, payloadBitLen);
            
            // Sketch phase: update the sketch with all update keys
            for (BigInteger data : updateKeys) {
                TripletZ2Vector[] shareKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(keyBitLen, data)});
                ssParty.update(SSTable, shareKey);
            }
            // Open and parse the sketch result
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) SSTable.getSketchTable());
            BigInteger[] sketchKeys = ZlDatabase.create(EnvType.STANDARD, true,
                    Arrays.copyOf(plainSketchRes, keyBitLen)).getBigIntegerData();
            BigInteger[] sketchCounts = ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(plainSketchRes, keyBitLen, keyBitLen + payloadBitLen)).getBigIntegerData();
            sketchRes = Pair.of(sketchKeys, sketchCounts);
            
            // Query phase: retrieve top-k frequent items
            TripletZ2Vector[] queryResult = (TripletZ2Vector[]) ssParty.getQuery(SSTable, topK);
            BitVector[] plainQueryRes = abb3Party.getZ2cParty().open(queryResult);
            BigInteger[] queryKeys = ZlDatabase.create(EnvType.STANDARD, true, Arrays.copyOf(plainQueryRes, keyBitLen)).getBigIntegerData();
            BigInteger[] queryCounts = ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(plainQueryRes, keyBitLen, keyBitLen + payloadBitLen)).getBigIntegerData();
            queryRes = Pair.of(queryKeys, queryCounts);

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
}

package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * Thread wrapper for CMS party execution in 3PC (Three-Party Computation).
 * This class encapsulates the CMS sketch operations performed by each party in the secure computation.
 */
public class CMSPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(CMSPartyThread.class);
    /**
     * CMS party instance handling the sketch operations
     */
    private final CMSParty cmsParty;
    /**
     * ABB3 party providing the underlying secure computation primitives
     */
    private final Abb3Party abb3Party;
    /**
     * Logarithm of the sketch table size (table size = 2^logSketchSize)
     */
    private final int logSketchSize;
    /**
     * Bit length of the payload (count values stored in the sketch)
     */
    private final int payloadBitLen;
    /**
     * Bit length of the input elements
     */
    private final int elementBitLen;
    /**
     * Array of keys to be inserted into the sketch during update phase
     */
    private final BigInteger[] updateKeys;
    /**
     * Array of keys to be queried from the sketch
     */
    private final BigInteger[] queryKeys;
    /**
     * Result of the sketch operation (the sketch table after all updates)
     */
    private long[] sketchRes;
    /**
     * Result of the query operation (counts for each queried key)
     */
    private long[] queryRes;
    /**
     * Hash parameters used for computing sketch indices
     */
    private HashParameters hashParameters;

    /**
     * Hash key used for CMSv2 implementation
     */
    private PlainZ2Vector hashKey;
    
    /**
     * Gets the hash key used for CMSv2
     * @return the hash key as PlainZ2Vector
     */
    PlainZ2Vector getHashKey() {
        return hashKey;
    }

    /**
     * Constructs a CMS party thread with the specified parameters
     * @param cmsParty the CMS party instance
     * @param updateKeys array of keys to update
     * @param queryKeys array of keys to query
     * @param elementBitLen bit length of elements
     * @param logSketchSize log of sketch table size
     * @param payloadBitLen bit length of payload
     */
    public CMSPartyThread(CMSParty cmsParty, BigInteger[] updateKeys, BigInteger[] queryKeys, int elementBitLen, int logSketchSize, int payloadBitLen) {
        this.cmsParty = cmsParty;
        abb3Party = cmsParty.getAbb3Party();
        this.updateKeys = updateKeys;
        this.queryKeys = queryKeys;
        this.logSketchSize = logSketchSize;
        this.elementBitLen = elementBitLen;
        this.payloadBitLen = payloadBitLen;
    }

    /**
     * Gets the sketch result
     * @return array representing the sketch table
     */
    public long[] getSketchRes() {
        return sketchRes;
    }

    /**
     * Gets the query result
     * @return array of counts for queried keys
     */
    public long[] getQueryRes() {
        return queryRes;
    }

    /**
     * Gets the hash parameters
     * @return hash parameters object
     */
    public HashParameters getHashParameters() {
        return hashParameters;
    }

    /**
     * Main execution method for the thread
     * Performs sketch initialization, updates, and queries
     */
    public void run() {
        try{
            // Initialize the CMS party
            cmsParty.init();
            // Create hash keys for the sketch
            TripletZ2Vector encKey = (TripletZ2Vector) abb3Party.getZ2cParty().createShareRandom(CommonConstants.BLOCK_BIT_LENGTH);
            BitVector plainKey= abb3Party.getZ2cParty().open(new TripletZ2Vector[]{encKey})[0];
            this.hashKey=PlainZ2Vector.create(plainKey);
            // Generate random hash parameters (a and b values)
            long[] plainAndB = new long[]{0, 0};
            while ((plainAndB[0] == 0 || plainAndB[1] == 0)) {
                TripletLongVector aAndB = abb3Party.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{2})[0];
                plainAndB = abb3Party.getLongParty().open(aAndB)[0].getElements();
            }
            hashParameters = new HashParameters(plainAndB[0], plainAndB[1], encKey);

            // Initialize the sketch table with shared zeros
            TripletZ2Vector[] shareData = IntStream.range(0, payloadBitLen)
                .mapToObj(i -> abb3Party.getZ2cParty().createShareZeros(1 << logSketchSize))
                .toArray(TripletZ2Vector[]::new);
            AbstractCMSTable cmsTable = new Z2CMSTable(shareData, payloadBitLen, elementBitLen, logSketchSize, hashParameters);
            
            // Sketch phase: update the sketch with all update keys
            for (BigInteger data : updateKeys) {
                TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[]{BitVectorFactory.create(elementBitLen, data)});
                cmsParty.update(cmsTable, currentKey);
            }
            // Open and get the sketch result
            BitVector[] plainSketchRes = abb3Party.getZ2cParty().open((TripletZ2Vector[]) cmsTable.getSketchTable());
            sketchRes = MatrixUtils.transBvIntoAv(plainSketchRes, abb3Party.getEnvType(), abb3Party.getParallel()).getElements();
            
            // Query phase: query the sketch for all query keys
            TripletZ2Vector[] secretQueryRes = IntStream.range(0, payloadBitLen)
                .mapToObj(i -> abb3Party.getZ2cParty().createEmpty(false))
                .toArray(TripletZ2Vector[]::new);
            for (BigInteger data : queryKeys) {
                TripletZ2Vector[] currentKey = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(new BitVector[] {BitVectorFactory.create(elementBitLen, data)});
                TripletZ2Vector[] oneQueryRes = (TripletZ2Vector[]) cmsParty.getQuery(cmsTable, currentKey);
                for (int i = 0; i < payloadBitLen; i++) {
                    secretQueryRes[i].merge(oneQueryRes[i]);
                }
            }
            // Open and get the query result
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
}

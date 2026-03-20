package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.ArrayList;

/**
 * Concrete implementation of HyperLogLog (HLL) sketch table for Z2 Boolean circuits.
 * 
 * This class extends AbstractHLLTable to provide HLL functionality using Z2 vectors.
 * It maintains the sketch counters and parameters specific to the Z2 implementation.
 * 
 * Key Parameters:
 * - payloadBitLen: Bit length of each counter, sufficient to store max leading ones count
 * - hashBitLen: Bit length of hash output h2(k), used for leading ones computation
 * 
 * The payloadBitLen is calculated as ceil(log2(hashBitLen)) since the maximum
 * number of leading ones in a hashBitLen-bit value is hashBitLen.
 */
public class HLLTable extends AbstractHLLTable {

    /**
     * Bit length of each counter in the sketch table.
     * This determines the maximum value each counter can store.
     * Calculated as ceil(log2(hashBitLen)) to accommodate the maximum possible leading ones count.
     */
    private final int payloadBitLen;

    /**
     * Bit length of the hash function h2 output.
     * This is the length of the second hash value used for computing leading ones.
     * A larger hashBitLen provides better precision but requires more storage.
     */
    private final int hashBitLen;

    /**
     * Constructs an HLLTable with existing data and buffer.
     * 
     * @param data the existing sketch table data as Z2 vectors
     * @param buffer the existing buffer containing batched updates
     * @param hashBitLen bit length of hash function h2 output
     * @param elementBitLen bit length of input elements
     * @param logSketchSize logarithm of sketch table size (table size = 2^logSketchSize)
     * @param encKey encryption key for hash computation
     */
    public HLLTable(MpcZ2Vector[] data, ArrayList<MpcVector> buffer, int hashBitLen, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        super(data, buffer, elementBitLen, logSketchSize, encKey);
        this.hashBitLen = hashBitLen;
        // Calculate payload bit length as ceil(log2(hashBitLen)) to store max leading ones
        this.payloadBitLen = LongUtils.ceilLog2(hashBitLen);
    }

    /**
     * Constructs an HLLTable with existing data and empty buffer.
     * 
     * @param data the existing sketch table data as Z2 vectors
     * @param hashBitLen bit length of hash function h2 output
     * @param elementBitLen bit length of input elements
     * @param logSketchSize logarithm of sketch table size (table size = 2^logSketchSize)
     * @param encKey encryption key for hash computation
     */
    public HLLTable(MpcZ2Vector[] data, int hashBitLen, int elementBitLen, int logSketchSize, MpcZ2Vector encKey) {
        super(data, elementBitLen, logSketchSize, encKey);
        this.hashBitLen = hashBitLen;
        // Calculate payload bit length as ceil(log2(hashBitLen)) to store max leading ones
        this.payloadBitLen = LongUtils.ceilLog2(hashBitLen);
    }

    /**
     * Gets the bit length of each counter.
     * 
     * @return the payload bit length
     */
    public int getPayloadBitLen() {
        return payloadBitLen;
    }

    /**
     * Gets the bit length of the hash function h2 output.
     * 
     * @return the hash bit length
     */
    public int getHashBitLen() {
        return hashBitLen;
    }

}

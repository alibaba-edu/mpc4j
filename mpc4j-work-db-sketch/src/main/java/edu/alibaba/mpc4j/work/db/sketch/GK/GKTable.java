package edu.alibaba.mpc4j.work.db.sketch.GK;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * Concrete implementation of GK (Greenwald-Khanna) sketch table for the S³ framework.
 * <p>
 * This class extends AbstractGKTable with Z2 vector support and maintains the
 * bit lengths for keys and attributes. The table structure is:
 * [key_bits, g1_bits, g2_bits, delta1_bits, delta2_bits, t_bits, dummy_flag]
 * <p>
 * The GK sketch provides ε-approximate quantile queries with space complexity
 * O((1/ε) log(εn)) for stream size n.
 * <p>
 * Reference: "Sketch-based Secure Query Processing for Streaming Data" (S³ framework)
 */
public class GKTable extends AbstractGKTable {
    /**
     * Bit length of the key values in the sketch.
     * Determines the precision of stored keys.
     */
    private final int keyBitLen;

    /**
     * Bit length for attributes (g1, g2, delta1, delta2, t).
     * These attributes track rank ranges and uncertainty bounds.
     */
    private final int attributeBitLen;

    /**
     * Constructs a GK table with the specified parameters.
     *
     * @param data the initial sketch table data as Z2 vectors
     * @param sketchSize the initial size of the sketch table
     * @param keyBitLen the bit length for key values
     * @param attributeBitLen the bit length for attribute values
     * @param epsilon the error parameter ε for approximation guarantee
     */
    public GKTable(MpcZ2Vector[] data, int sketchSize, int keyBitLen, int attributeBitLen, double epsilon) {
        super(data);
        this.sketchSize = sketchSize;
        this.keyBitLen = keyBitLen;
        this.attributeBitLen = attributeBitLen;
        this.epsilon = epsilon;
    }

    /**
     * Gets the bit length of key values.
     *
     * @return the key bit length
     */
    public int getKeyBitLen() {
        return keyBitLen;
    }

    /**
     * Gets the bit length of attribute values.
     *
     * @return the attribute bit length
     */
    public int getAttributeBitLen() {
        return attributeBitLen;
    }

    /**
     * Calculates the threshold for GK sketch compaction.
     * <p>
     * The threshold is ε*n where n is the total stream size.
     * This threshold determines which tuples can be merged during compaction.
     *
     * @return the compaction threshold
     */
    public int getThreshold() {
        return (int) (epsilon * dataSize);
    }
}

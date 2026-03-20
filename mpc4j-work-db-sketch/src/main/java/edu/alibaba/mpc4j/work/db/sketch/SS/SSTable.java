package edu.alibaba.mpc4j.work.db.sketch.SS;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * SpaceSaving (SS) sketch table implementation using Z2 Boolean circuits.
 *
 * <p>This concrete implementation extends AbstractSSTable and specifies the bit lengths
 * for keys and values. It uses Z2 vectors (Boolean circuits) for secure computation
 * of SS operations in the MPC setting.
 *
 * <p>The table structure:
 * - First keyBitLen vectors: represent the key (item identifier) in binary
 * - Next payloadBitLen vectors: represent the frequency/count value in binary
 *
 * <p>Example: If keyBitLen=16 and payloadBitLen=32, each entry uses 48 bit vectors
 * to store a 16-bit key and 32-bit frequency.
 */
public class SSTable extends AbstractSSTable {
    /**
     * Payload bit length (value bit length).
     *
     * <p>This specifies the number of bits used to represent the frequency/count value.
     * Larger values can store higher frequencies but require more storage and computation.
     *
     * <p>Typical values: 16-64 bits depending on expected stream length and accuracy requirements.
     */
    private final int payloadBitLen;

    /**
     * Key bit length.
     *
     * <p>This specifies the number of bits used to represent the key (item identifier).
     * The key space is 2^keyBitLen, which determines the range of possible stream elements.
     *
     * <p>Typical values: 8-64 bits depending on the domain of stream elements.
     */
    private final int keyBitLen;

    /**
     * Constructor for SS table with Z2 vectors.
     *
     * @param data          the Z2 vectors containing key-value pairs
     * @param logSketchSize the log of the sketch table size (size = 2^logSketchSize)
     * @param keyBitLen     the number of bits for representing keys
     * @param payloadBitLen the number of bits for representing frequency values
     */
    public SSTable(MpcZ2Vector[] data, int logSketchSize, int keyBitLen, int payloadBitLen) {
        super(data);
        this.logSketchSize = logSketchSize;
        this.keyBitLen = keyBitLen;
        this.payloadBitLen = payloadBitLen;
    }

    /**
     * Get the payload bit length.
     *
     * @return the number of bits used for frequency values
     */
    public int getPayloadBitLen() {
        return payloadBitLen;
    }

    /**
     * Get the key bit length.
     *
     * @return the number of bits used for keys
     */
    public int getKeyBitLen() {
        return keyBitLen;
    }
}

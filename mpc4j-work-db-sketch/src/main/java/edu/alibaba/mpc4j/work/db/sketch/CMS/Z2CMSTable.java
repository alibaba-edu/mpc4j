package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * Z2 Boolean circuit implementation of the Count-Min Sketch (CMS) table in the S³ framework.
 * 
 * <p>This class extends the abstract CMS table for Z2 arithmetic, where all operations
 * are performed using binary circuits. The frequency counts are represented as binary
 * vectors with a specified bit length, allowing secure computation using Z2 MPC protocols.</p>
 * 
 * <p>The Z2 CMS table is used with the CMSz2Party protocol, which implements secure
 * CMS operations (update and query) using Z2 Boolean circuits and oblivious primitives
 * like sorting, permutation, and group-by-sum.</p>
 */
public class Z2CMSTable extends AbstractCMSTable {
    /**
     * The bit length for payload (frequency counts).
     * 
     * <p>This specifies the number of bits used to represent each frequency count.
     * The maximum count value that can be stored is 2^payloadBitLen - 1.
     * For example, if payloadBitLen = 16, the maximum count is 65535.</p>
     */
    private final int payloadBitLen;

    /**
     * Constructs a Z2 CMS table with the specified parameters.
     * 
     * @param data            the initial sketch table data as Z2 vectors (secret-shared binary counts)
     * @param payloadBitLen   the bit length for payload (frequency counts)
     * @param elementBitLen   the bit length of input elements (keys)
     * @param logSketchSize   the base-2 logarithm of the sketch table size
     * @param hashParameters  the hash parameters for computing key hashes
     */
    public Z2CMSTable(MpcZ2Vector[] data, int payloadBitLen, int elementBitLen, int logSketchSize, HashParameters hashParameters) {
        super(data, elementBitLen, logSketchSize, hashParameters);
        this.payloadBitLen = payloadBitLen;
    }

    /**
     * Gets the bit length for payload (frequency counts).
     * 
     * @return the number of bits used to represent each frequency count
     */
    public int getPayloadBitLen() {
        return payloadBitLen;
    }

}

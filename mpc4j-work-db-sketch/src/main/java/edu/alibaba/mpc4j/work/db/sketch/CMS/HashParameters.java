package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * Hash parameters for the Count-Min Sketch (CMS) protocol in the S³ framework.
 * 
 * <p>This class encapsulates the parameters needed for hash computation in CMS,
 * supporting both arithmetic and binary hashing schemes. CMS uses multiple
 * hash functions h_i(k) = (a_i * k + b_i) mod s to map keys to table indices.</p>
 * 
 * <p>The parameters include:
 * - Arithmetic hashing: coefficients a and b for computing h(k) = (a*k + b) mod s
 * - Binary hashing: encryption key for secure oblivious permutation (SOPRP)</p>
 */
public class HashParameters {
    /**
     * Coefficient 'a' in the arithmetic hash function h(k) = (a*k + b) mod s.
     * 
     * <p>This coefficient scales the key before adding the offset.</p>
     */
    private final long a;
    /**
     * Coefficient 'b' in the arithmetic hash function h(k) = (a*k + b) mod s.
     * 
     * <p>This coefficient provides an offset to the scaled key.</p>
     */
    private final long b;
    /**
     * Encryption key for secure oblivious permutation (SOPRP) in binary hashing.
     * 
     * <p>This secret-shared key is used for oblivious permutation of binary values,
     * enabling secure hash computation in the Z2 circuit implementation.</p>
     */
    private final MpcZ2Vector encKey;

    /**
     * Constructs hash parameters with arithmetic coefficients only.
     * 
     * @param a the coefficient 'a' in h(k) = (a*k + b) mod s
     * @param b the coefficient 'b' in h(k) = (a*k + b) mod s
     */
    public HashParameters(long a, long b) {
        this.a = a;
        this.b = b;
        this.encKey = null;
    }

    /**
     * Constructs hash parameters with a binary encryption key only.
     * 
     * @param encKey the encryption key for SOPRP in binary hashing
     */
    public HashParameters(MpcZ2Vector encKey) {
        this.a = 0;
        this.b = 0;
        this.encKey = encKey;
    }

    /**
     * Constructs hash parameters with both arithmetic coefficients and binary encryption key.
     * 
     * @param a      the coefficient 'a' in h(k) = (a*k + b) mod s
     * @param b      the coefficient 'b' in h(k) = (a*k + b) mod s
     * @param encKey the encryption key for SOPRP in binary hashing
     */
    public HashParameters(long a, long b, MpcZ2Vector encKey) {
        this.a = a;
        this.b = b;
        this.encKey = encKey;
    }

    /**
     * Gets coefficient 'a' in the arithmetic hash function.
     * 
     * @return the coefficient 'a'
     */
    public long getA() {
        return a;
    }

    /**
     * Gets coefficient 'b' in the arithmetic hash function.
     * 
     * @return the coefficient 'b'
     */
    public long getB() {
        return b;
    }

    /**
     * Gets the encryption key for binary SOPRP.
     * 
     * @return the secret-shared encryption key
     */
    public MpcZ2Vector getEncKey() {
        return encKey;
    }
}

package edu.alibaba.mpc4j.work.db.sketch.CMS;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;

/**
 * Hash parameters in CMS protocol
 */
public class HashParameters {
    /**
     * a and b in hash function for arithmetic values
     */
    private final long a;
    private final long b;
    /**
     * enc key of soprp for binary values
     */
    private final MpcZ2Vector encKey;

    public HashParameters(long a, long b) {
        this.a = a;
        this.b = b;
        this.encKey = null;
    }

    public HashParameters(MpcZ2Vector encKey) {
        this.a = 0;
        this.b = 0;
        this.encKey = encKey;
    }

    public HashParameters(long a, long b, MpcZ2Vector encKey) {
        this.a = a;
        this.b = b;
        this.encKey = encKey;
    }

    public long getA() {
        return a;
    }

    public long getB() {
        return b;
    }

    public MpcZ2Vector getEncKey() {
        return encKey;
    }
}

package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

/**
 * Simple PIR scheme params with 128-bit security.
 *
 * @author Liqiang Peng
 * @date 2023/5/26
 */
public class Hhcm23SimpleSingleIndexPirParams implements SingleIndexPirParams {
    /**
     * the integer modulus
     */
    public long q;
    /**
     * the plaintext modulus
     */
    public int p;
    /**
     * the lwe secret length
     */
    public int n;
    /**
     * the standard deviation for sampling random elements
     */
    public double stdDev;
    /**
     * zl64
     */
    public Zl64 zl64;
    /**
     * bit length of p
     */
    public int logP = 10;

    public Hhcm23SimpleSingleIndexPirParams(int n, int modulusBitLength, double stdDev) {
        this.n = n;
        this.q = 1L << modulusBitLength;
        this.stdDev = stdDev;
        this.zl64 = Zl64Factory.createInstance(EnvType.STANDARD_JDK, modulusBitLength);
    }

    /**
     * default params
     */
    public static Hhcm23SimpleSingleIndexPirParams DEFAULT_PARAMS = new Hhcm23SimpleSingleIndexPirParams(1024, 32, 6.4);

    /**
     * set plain modulo.
     *
     * @param m cols.
     */
    public void setPlainModulo(int m) {
        if (m <= 13) {
            this.p = 991;
        } else if (m == 14) {
            this.p = 833;
        } else if (m == 15) {
            this.p = 701;
        } else if (m == 16) {
            this.p = 589;
        } else {
            assert false : "failed to generate Simple PIR params.";
        }
    }

    @Override
    public int getPlainModulusBitLength() {
        return 0;
    }

    @Override
    public int getPolyModulusDegree() {
        return 0;
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public byte[] getEncryptionParams() {
        return null;
    }

    @Override
    public String toString() {
        return
            "LWE encryption parameters : " + "\n" +
                " - n : " + n + "\n" +
                " - q : " + q + "\n" +
                " - std-dev : " + stdDev;
    }



}

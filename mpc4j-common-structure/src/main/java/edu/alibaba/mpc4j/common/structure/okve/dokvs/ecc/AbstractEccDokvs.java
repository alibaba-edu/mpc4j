package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;

import java.security.SecureRandom;

/**
 * abstract DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
abstract class AbstractEccDokvs<T> implements EccDokvs<T> {
    /**
     * number of key-value pairs.
     */
    protected final int n;
    /**
     * size of encode storage with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
    /**
     * ECC API
     */
    protected final Ecc ecc;
    /**
     * Zp
     */
    protected final Zp zp;
    /**
     * the random state
     */
    protected final SecureRandom secureRandom;
    /**
     * parallel encode
     */
    protected boolean parallelEncode;

    protected AbstractEccDokvs(EnvType envType, Ecc ecc, int n, int m, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        this.ecc = ecc;
        zp = ZpFactory.createInstance(envType, ecc.getN());
        // m >= n, and m % Byte.SIZE == 0
        MathPreconditions.checkGreaterOrEqual("m", m, n);
        MathPreconditions.checkEqual("m % Byte.SIZE", "0", m % Byte.SIZE, 0);
        this.m = m;
        this.secureRandom = secureRandom;
        parallelEncode = false;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        this.parallelEncode = parallelEncode;
    }

    @Override
    public boolean getParallelEncode() {
        return parallelEncode;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() {
        return m;
    }
}

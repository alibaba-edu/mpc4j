package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * abstract DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/2/19
 */
abstract class AbstractZpDokvs<T> implements ZpDokvs<T> {
    /**
     * number of key-value pairs.
     */
    protected final int n;
    /**
     * size of encode storage with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
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

    protected AbstractZpDokvs(EnvType envType, BigInteger p, int n, int m, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        zp = ZpFactory.createInstance(envType, p);
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
    public int getM() {return m;}
}

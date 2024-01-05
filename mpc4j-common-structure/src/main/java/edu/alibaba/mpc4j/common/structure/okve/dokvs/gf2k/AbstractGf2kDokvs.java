package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;

import java.security.SecureRandom;

/**
 * abstract GF2K-DOKVS.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
abstract class AbstractGf2kDokvs<T> implements Gf2kDokvs<T> {
    /**
     * GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * number of key-value pairs.
     */
    protected final int n;
    /**
     * size of encode storage with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
    /**
     * the random state
     */
    protected final SecureRandom secureRandom;
    /**
     * parallel encode
     */
    protected boolean parallelEncode;

    protected AbstractGf2kDokvs(EnvType envType, int n, int m, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        // m >= n, and m % Byte.SIZE == 0
        MathPreconditions.checkGreater("m", m, n);
        Preconditions.checkArgument(m % Byte.SIZE == 0, "m must divide " + Byte.SIZE + ": " + m);
        this.m = m;
        this.secureRandom = secureRandom;
        parallelEncode = false;
        // create GF2K instance
        gf2k = Gf2kFactory.createInstance(envType);
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

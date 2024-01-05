package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;

/**
 * abstract GF(2^e)-DOKVS.
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
abstract class AbstractGf2eDokvs<T> implements Gf2eDokvs<T> {
    /**
     * number of key-value pairs.
     */
    protected final int n;
    /**
     * size of encode storage with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
    /**
     * m in byte
     */
    protected final int byteM;
    /**
     * bit length of values
     */
    protected final int l;
    /**
     * l in byte
     */
    protected final int byteL;
    /**
     * the random state
     */
    protected final SecureRandom secureRandom;
    /**
     * parallel encode
     */
    protected boolean parallelEncode;

    protected AbstractGf2eDokvs(int n, int m, int l, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        // here we only need to require l > 0
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // m >= n, and m % Byte.SIZE == 0
        MathPreconditions.checkGreater("m", m, n);
        Preconditions.checkArgument(m % Byte.SIZE == 0, "m must divide " + Byte.SIZE + ": " + m);
        this.m = m;
        byteM = m / Byte.SIZE;
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

    @Override
    public int getL() {
        return l;
    }
}

package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.tool.ZpLinearSolver;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * abstract OVDM in Zp.
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
public abstract class AbstractZpOvdm<T> implements ZpOvdm<T> {
    /**
     * number of key-value pairs
     */
    protected final int n;
    /**
     * OVDM length, with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
    /**
     * m in bytes
     */
    final int mByteLength;
    /**
     * Zp
     */
    protected final Zp zp;
    /**
     * Zp linear solver
     */
    protected final ZpLinearSolver zpLinearSolver;
    /**
     * the random state
     */
    protected final SecureRandom secureRandom;

    protected AbstractZpOvdm(EnvType envType, BigInteger prime, int n, int m) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        zp = ZpFactory.createInstance(envType, prime);
        zpLinearSolver = new ZpLinearSolver(zp);
        // m >= n, and m % Byte.SIZE == 0
        MathPreconditions.checkGreaterOrEqual("m", m, n);
        MathPreconditions.checkEqual("m % Byte.SIZE", "0", m % Byte.SIZE, 0);
        this.m = m;
        mByteLength = CommonUtils.getByteLength(m);
        secureRandom = new SecureRandom();
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() { return m; }
}

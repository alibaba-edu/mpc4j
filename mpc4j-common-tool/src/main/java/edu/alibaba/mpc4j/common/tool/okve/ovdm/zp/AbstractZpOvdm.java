package edu.alibaba.mpc4j.common.tool.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpLinearSolver;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Zp-OVDM抽象类。
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
public abstract class AbstractZpOvdm<T> implements ZpOvdm<T> {
    /**
     * OVDM允许编码的键值对数量
     */
    protected final int n;
    /**
     * OVDM长度，满足{@code m % Byte.SIZE == 0}。
     */
    protected final int m;
    /**
     * m的字节长度
     */
    final int mByteLength;
    /**
     * 模数p
     */
    protected final BigInteger prime;
    /**
     * Zp线性求解器
     */
    protected final ZpLinearSolver zpLinearSolver;
    /**
     * 编码过程所用到的随机状态
     */
    protected final SecureRandom secureRandom;

    protected AbstractZpOvdm(BigInteger prime, int n, int m) {
        assert n > 0;
        this.n = n;
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH);
        this.prime = prime;
        zpLinearSolver = new ZpLinearSolver(prime);
        // 要求m >= n，且m可以被Byte.SIZE整除
        assert m >= n && m % Byte.SIZE == 0;
        this.m = m;
        mByteLength = m / Byte.SIZE;
        secureRandom = new SecureRandom();
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() { return m; }

    @Override
    public BigInteger getPrime() {
        return prime;
    }
}

package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Manager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Zp64多项式差值抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
abstract class AbstractZp64Poly implements Zp64Poly {
    /**
     * 有限域模数p
     */
    protected final long p;
    /**
     * 有限域比特长度
     */
    private final int l;

    AbstractZp64Poly(int l) {
        p = Zp64Manager.getPrime(l);
        this.l = l;
    }

    AbstractZp64Poly(long p) {
        assert BigInteger.valueOf(p).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "p is probably not prime: " + p;
        this.p = p;
        this.l = LongUtils.ceilLog2(p) - 1;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public long getPrime() {
        return p;
    }

    protected boolean validPoint(long point) {
        return point >= 0 && point < p;
    }
}

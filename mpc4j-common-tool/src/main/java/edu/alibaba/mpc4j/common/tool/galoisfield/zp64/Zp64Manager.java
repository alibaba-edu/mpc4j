package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Zp64有限域管理器。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class Zp64Manager {
    /**
     * 私有构造函数
     */
    private Zp64Manager() {
        // empty
    }

    /**
     * 返回比特长度l所对应的有限域Zp64。
     *
     * @param l Zp有限域比特长度。
     * @return 有限域Zp。
     */
    public static IntegersZp64 getFiniteField(int l) {
        assert l > 0 && l <= LongUtils.MAX_L : "l must be in range (0, " + LongUtils.MAX_L + "]:" + l;
        return new IntegersZp64(getPrime(l));
    }

    /**
     * 返回比特长度l所对应的有限域质数p。
     *
     * @param l Zp有限域比特长度。
     * @return 有限域质数p。
     */
    public static long getPrime(int l) {
        assert l > 0 && l <= LongUtils.MAX_L : "l must be in range (0, " + LongUtils.MAX_L + "]:" + l;
        BigInteger bigIntegerPrime = ZpManager.getPrime(l);
        return bigIntegerPrime.longValue();
    }
}

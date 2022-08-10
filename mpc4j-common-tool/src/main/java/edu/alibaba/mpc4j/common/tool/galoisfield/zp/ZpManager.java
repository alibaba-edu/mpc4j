package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import cc.redberry.rings.IntegersZp;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Zp有限域管理器。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
public class ZpManager {
    /**
     * 默认可能用到的GF(2^l)有限域数量
     */
    private static final int DEFAULT_SIZE = 10;
    /**
     * 有限域映射表
     */
    private static final Map<Integer, IntegersZp> ZP_MAP = new HashMap<>(DEFAULT_SIZE);
    /**
     * 有限域对应质数的映射表
     */
    private static final Map<Integer, BigInteger> ZP_PRIME_MAP = new HashMap<>(DEFAULT_SIZE);

    /**
     * 私有构造函数
     */
    private ZpManager() {
        // empty
    }

    /**
     * 返回比特长度l所对应的有限域Zp。
     *
     * @param l Zp有限域比特长度。
     * @return 有限域Zp。
     */
    public static IntegersZp getFiniteField(int l) {
        assert l > 0 : "l must be greater than 0: " + l;
        if (ZP_MAP.containsKey(l)) {
            return ZP_MAP.get(l);
        } else {
            BigInteger p = BigIntegerUtils.BIGINT_2.pow(l).nextProbablePrime();
            IntegersZp finiteField = new IntegersZp(new cc.redberry.rings.bigint.BigInteger(p));
            ZP_MAP.put(l, finiteField);
            ZP_PRIME_MAP.put(l, p);
            return finiteField;
        }
    }

    /**
     * 返回比特长度l所对应的有限域质数p。
     *
     * @param l Zp有限域比特长度。
     * @return 有限域质数p。
     */
    public static BigInteger getPrime(int l) {
        assert l > 0 : "l must be greater than 0: " + l;
        if (ZP_PRIME_MAP.containsKey(l)) {
            return ZP_PRIME_MAP.get(l);
        } else {
            BigInteger p = BigIntegerUtils.BIGINT_2.pow(l).nextProbablePrime();
            IntegersZp finiteField = new IntegersZp(new cc.redberry.rings.bigint.BigInteger(p));
            ZP_MAP.put(l, finiteField);
            ZP_PRIME_MAP.put(l, p);
            return p;
        }
    }
}

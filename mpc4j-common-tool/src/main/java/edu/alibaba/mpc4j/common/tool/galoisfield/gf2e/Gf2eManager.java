package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.Rings;
import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * GF(2^l)有限域管理器。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class Gf2eManager {
    /**
     * 默认可能用到的GF(2^l)有限域数量
     */
    private static final int DEFAULT_SIZE = 10;
    /**
     * GF(2^128)使用的模数为f(x) = x^128 + x^7 + x^2 + x + 1
     */
    private static final UnivariatePolynomialZp64 GF2K_POLYNOMIAL = UnivariatePolynomialZp64.create(
        2, new long[] {
            1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            1L,
        }
    );
    /**
     * 有限域映射表
     */
    private static final Map<Integer, FiniteField<UnivariatePolynomialZp64>> GF2X_MAP = new HashMap<>(DEFAULT_SIZE);

    static {
        GF2X_MAP.put(CommonConstants.BLOCK_BIT_LENGTH, Rings.GF(GF2K_POLYNOMIAL));
    }

    /**
     * 私有构造函数
     */
    private Gf2eManager() {
        // empty
    }

    /**
     * 返回比特长度l所对应的有限域GF(2^l)。
     *
     * @param l GF(2^l)有限域比特长度。
     * @return 有限域GF(2^l)。
     */
    public static FiniteField<UnivariatePolynomialZp64> getFiniteField(int l) {
        assert l > 0 : "l must be greater than 0: " + l;
        if (GF2X_MAP.containsKey(l)) {
            return GF2X_MAP.get(l);
        } else {
            FiniteField<UnivariatePolynomialZp64> finiteField = Rings.GF(2, l);
            GF2X_MAP.put(l, finiteField);

            return finiteField;
        }
    }
}

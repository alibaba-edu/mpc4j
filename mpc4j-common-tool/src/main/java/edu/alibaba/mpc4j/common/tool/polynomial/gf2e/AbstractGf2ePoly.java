package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;

/**
 * GF2E多项式插值抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
abstract class AbstractGf2ePoly implements Gf2ePoly {
    /**
     * 有限域
     */
    protected final FiniteField<UnivariatePolynomialZp64> finiteField;
    /**
     * 有限域比特长度
     */
    protected final int l;
    /**
     * 有限域字节长度
     */
    protected final int byteL;
    /**
     * 随机状态
     */
    protected final SecureRandom secureRandom;

    AbstractGf2ePoly(int l) {
        finiteField = Gf2eManager.getFiniteField(l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        secureRandom = new SecureRandom();
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getL() {
        return l;
    }

    protected boolean validPoint(byte[] point) {
        return point.length == byteL && BytesUtils.isReduceByteArray(point, l);
    }
}

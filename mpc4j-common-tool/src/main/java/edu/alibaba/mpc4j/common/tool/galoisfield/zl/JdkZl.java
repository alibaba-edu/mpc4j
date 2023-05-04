package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * The Zl implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
class JdkZl extends AbstractZl {
    /**
     * module using AND operation
     */
    private final BigInteger andModule;

    JdkZl(EnvType envType, int l) {
        super(envType, l);
        andModule = rangeBound.subtract(BigInteger.ONE);
    }

    @Override
    public ZlFactory.ZlType getZlType() {
        return ZlFactory.ZlType.JDK;
    }

    @Override
    public BigInteger module(BigInteger a) {
        return a.and(andModule);
    }

    @Override
    public BigInteger add(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.add(b).and(andModule);
    }

    @Override
    public BigInteger neg(final BigInteger a) {
        assert validateElement(a);
        if (a.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            return rangeBound.subtract(a);
        }
    }

    @Override
    public BigInteger sub(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.subtract(b).and(andModule);
    }

    @Override
    public BigInteger mul(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.multiply(b).and(andModule);
    }

    @Override
    public BigInteger pow(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert b.signum() >= 0;
        return BigIntegerUtils.modPow(a, b, rangeBound);
    }
}

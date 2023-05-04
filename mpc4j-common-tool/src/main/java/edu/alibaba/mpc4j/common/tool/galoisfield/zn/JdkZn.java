package edu.alibaba.mpc4j.common.tool.galoisfield.zn;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * Zn implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
class JdkZn extends AbstractZn {

    JdkZn(EnvType envType, BigInteger n) {
        super(envType, n);
    }

    @Override
    public ZnFactory.ZnType getZnType() {
        return ZnFactory.ZnType.JDK;
    }

    @Override
    public BigInteger module(final BigInteger a) {
        return a.mod(n);
    }

    @Override
    public BigInteger add(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.add(b).mod(n);
    }

    @Override
    public BigInteger neg(final BigInteger a) {
        assert validateElement(a);
        if (a.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            return n.subtract(a);
        }
    }

    @Override
    public BigInteger sub(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.subtract(b).mod(n);
    }

    @Override
    public BigInteger mul(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.multiply(b).mod(n);
    }

    @Override
    public BigInteger pow(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert b.signum() >= 0;
        return BigIntegerUtils.modPow(a, b, n);
    }
}

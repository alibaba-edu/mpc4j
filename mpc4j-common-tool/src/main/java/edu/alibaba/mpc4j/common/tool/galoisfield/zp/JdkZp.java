package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * Zp implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
class JdkZp extends AbstractZp {

    JdkZp(EnvType envType, BigInteger prime) {
        super(envType, prime);
    }

    JdkZp(EnvType envType, int l) {
        super(envType, l);
    }

    @Override
    public ZpFactory.ZpType getZpType() {
        return ZpFactory.ZpType.JDK;
    }

    @Override
    public BigInteger module(final BigInteger a) {
        return a.mod(prime);
    }

    @Override
    public BigInteger add(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.add(b).mod(prime);
    }

    @Override
    public BigInteger neg(final BigInteger a) {
        assert validateElement(a);
        if (a.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            return prime.subtract(a);
        }
    }

    @Override
    public BigInteger sub(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.subtract(b).mod(prime);
    }

    @Override
    public BigInteger mul(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.multiply(b).mod(prime);
    }

    @Override
    public BigInteger div(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateNonZeroElement(b);
        return a.multiply(BigIntegerUtils.modInverse(b, prime)).mod(prime);
    }

    @Override
    public BigInteger inv(final BigInteger a) {
        assert validateNonZeroElement(a);
        return BigIntegerUtils.modInverse(a, prime);
    }

    @Override
    public BigInteger pow(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert b.signum() >= 0;
        return BigIntegerUtils.modPow(a, b, prime);
    }
}

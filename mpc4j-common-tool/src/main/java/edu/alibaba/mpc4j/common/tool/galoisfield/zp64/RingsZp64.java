package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zp64 implemented by Rings.
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class RingsZp64 extends AbstractZp64 {
    /**
     * the finite field
     */
    private final IntegersZp64 integersZp64;

    public RingsZp64(EnvType envType, long prime) {
        super(envType, prime);
        integersZp64 = new IntegersZp64(prime);
    }

    RingsZp64(EnvType envType, int l) {
        super(envType, l);
        integersZp64 = new IntegersZp64(prime);
    }

    @Override
    public Zp64Factory.Zp64Type getZp64Type() {
        return Zp64Factory.Zp64Type.RINGS;
    }

    @Override
    public long module(final long a) {
        return integersZp64.modulus(a);
    }

    @Override
    public long add(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.add(a, b);
    }

    @Override
    public long neg(final long a) {
        assert validateElement(a);
        return integersZp64.negate(a);
    }

    @Override
    public long sub(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.subtract(a, b);
    }

    @Override
    public long mul(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.multiply(a, b);
    }

    @Override
    public long div(final long a, final long b) {
        assert validateElement(a);
        assert validateNonZeroElement(b);
        return integersZp64.divide(a, b);
    }

    @Override
    public long inv(final long a) {
        assert validateNonZeroElement(a);
        return integersZp64.divide(1L, a);
    }

    @Override
    public long pow(final long a, final long b) {
        assert validateElement(a);
        assert b >= 0;
        return integersZp64.powMod(a, b);
    }
}

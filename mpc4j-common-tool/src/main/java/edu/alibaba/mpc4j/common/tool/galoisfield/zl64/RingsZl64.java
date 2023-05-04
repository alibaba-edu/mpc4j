package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zl64 implemented by Rings library.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
class RingsZl64 extends AbstractZl64 {
    /**
     * the finite ring
     */
    private final IntegersZp64 integersZp64;

    public RingsZl64(EnvType envType, int l) {
        super(envType, l);
        integersZp64 = new IntegersZp64(rangeBound);
    }

    @Override
    public Zl64Factory.Zl64Type getZl64Type() {
        return Zl64Factory.Zl64Type.RINGS;
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
    public long pow(final long a, final long b) {
        assert validateElement(a);
        assert b >= 0;
        return integersZp64.powMod(a, b);
    }
}

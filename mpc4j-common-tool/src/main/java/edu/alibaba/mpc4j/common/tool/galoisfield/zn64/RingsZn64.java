package edu.alibaba.mpc4j.common.tool.galoisfield.zn64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zn64 implemented by Rings.
 *
 * @author Weiran Liu
 * @date 2023/3/15
 */
class RingsZn64 extends AbstractZn64 {
    /**
     * the finite field
     */
    private final IntegersZp64 integersZp64;

    RingsZn64(EnvType envType, long n) {
        super(envType, n);
        integersZp64 = new IntegersZp64(n);
    }

    @Override
    public Zn64Factory.Zn64Type getZn64Type() {
        return Zn64Factory.Zn64Type.RINGS;
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

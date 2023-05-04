package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Zl64 implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
class JdkZl64 extends AbstractZl64 {
    /**
     * module using AND operation
     */
    private final long andModule;

    public JdkZl64(EnvType envType, int l) {
        super(envType, l);
        andModule = rangeBound - 1;
    }

    @Override
    public Zl64Factory.Zl64Type getZl64Type() {
        return Zl64Factory.Zl64Type.JDK;
    }

    @Override
    public long module(final long a) {
        return a & andModule;
    }

    @Override
    public long add(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a + b) & andModule;
    }

    @Override
    public long neg(final long a) {
        assert validateElement(a);
        return (-a) & andModule;
    }

    @Override
    public long sub(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a - b) & andModule;
    }

    @Override
    public long mul(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a * b) & andModule;
    }

    @Override
    public long pow(final long a, final long b) {
        assert validateElement(a);
        assert b >= 0;
        // this is exactly what Rings did. However, since the module is 2^l, we can use & instead of mod.
        if (b == 0) {
            return 1;
        }
        long result = 1;
        long exponent = b;
        long base2k = a;
        for (; ; ) {
            if ((exponent & 1) != 0) {
                result = (result * base2k) & andModule;
            }
            exponent = exponent >> 1;
            if (exponent == 0) {
                return result;
            }
            base2k = (base2k * base2k) & andModule;
        }
    }
}

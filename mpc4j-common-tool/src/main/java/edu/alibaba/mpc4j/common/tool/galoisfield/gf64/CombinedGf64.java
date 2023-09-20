package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;

/**
 * combined GF(2^64).
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
class CombinedGf64 extends AbstractGf64 {
    /**
     * NTL GF(2^64).
     */
    private final NtlGf64 ntlGf64;
    /**
     * JDK GF(2^64).
     */
    private final JdkGf64 jdkGf64;

    CombinedGf64(EnvType envType) {
        super(envType);
        ntlGf64 = new NtlGf64(envType);
        jdkGf64 = new JdkGf64(envType);
    }

    @Override
    public Gf64Type getGf64Type() {
        return Gf64Type.COMBINED;
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ntlGf64.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ntlGf64.divi(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return ntlGf64.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        ntlGf64.invi(p);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return jdkGf64.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        jdkGf64.muli(p, q);
    }
}

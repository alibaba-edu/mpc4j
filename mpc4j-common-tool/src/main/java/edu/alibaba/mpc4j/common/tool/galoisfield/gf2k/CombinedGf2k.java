package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * combined GF(2^128).
 *
 * @author Weiran Liu
 * @date 2023/7/2
 */
class CombinedGf2k extends AbstractGf2k {
    /**
     * NTL GF(2^κ).
     */
    private final NtlGf2k ntlGf2k;
    /**
     * JDK GF(2^κ).
     */
    private final JdkGf2k jdkGf2k;

    CombinedGf2k(EnvType envType) {
        super(envType);
        ntlGf2k = new NtlGf2k(envType);
        jdkGf2k = new JdkGf2k(envType);
    }

    @Override
    public Gf2kFactory.Gf2kType getGf2kType() {
        return Gf2kFactory.Gf2kType.COMBINED;
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ntlGf2k.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ntlGf2k.divi(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return ntlGf2k.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        ntlGf2k.invi(p);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return jdkGf2k.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        jdkGf2k.muli(p, q);
    }
}

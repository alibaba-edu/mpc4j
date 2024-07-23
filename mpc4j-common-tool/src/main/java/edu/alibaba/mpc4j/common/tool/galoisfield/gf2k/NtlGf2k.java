package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.NtlGf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * NTL GF(2^128).
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
class NtlGf2k extends AbstractGf2k {
    /**
     * NTL GF(2^128)
     */
    private final NtlGf2e ntlGf128;

    NtlGf2k(EnvType envType) {
        super(envType);
        ntlGf128 = new NtlGf2e(envType, 128);
    }

    @Override
    public Gf2eType getGf2eType() {
        return ntlGf128.getGf2eType();
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.NTL;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return ntlGf128.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        ntlGf128.muli(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return ntlGf128.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        ntlGf128.invi(p);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ntlGf128.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ntlGf128.divi(p, q);
    }
}

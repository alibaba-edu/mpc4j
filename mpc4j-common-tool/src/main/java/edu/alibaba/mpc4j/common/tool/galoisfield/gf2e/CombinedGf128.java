package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.JdkGf2k;

/**
 * Combined GF(2^128).
 *
 * @author Weiran Liu
 * @date 2024/6/4
 */
public class CombinedGf128 extends AbstractGf128 {
    /**
     * NTL GF(2^128).
     */
    private final NtlGf2e ntlGf128;
    /**
     * JDK GF(2^128).
     */
    private final JdkGf2k jdkGf128;

    public CombinedGf128(EnvType envType) {
        super(envType);
        ntlGf128 = new NtlGf2e(envType, 128);
        jdkGf128 = new JdkGf2k(envType);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.COMBINED;
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ntlGf128.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ntlGf128.divi(p, q);
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
    public byte[] mul(byte[] p, byte[] q) {
        return jdkGf128.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        jdkGf128.muli(p, q);
    }
}

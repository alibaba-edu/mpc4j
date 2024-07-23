package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;

/**
 * combined GF(2^64).
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
class CombinedGf064 extends AbstractGf064 {
    /**
     * NTL GF(2^64).
     */
    private final NtlGf2e ntlGf064;
    /**
     * JDK GF(2^64).
     */
    private final JdkGf064 jdkGf064;

    CombinedGf064(EnvType envType) {
        super(envType);
        ntlGf064 = new NtlGf2e(envType, 64);
        jdkGf064 = new JdkGf064(envType);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.COMBINED;
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ntlGf064.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ntlGf064.divi(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return ntlGf064.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        ntlGf064.invi(p);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return jdkGf064.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        jdkGf064.muli(p, q);
    }
}

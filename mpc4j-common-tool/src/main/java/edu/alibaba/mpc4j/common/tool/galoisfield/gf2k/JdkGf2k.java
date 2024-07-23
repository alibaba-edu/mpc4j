package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.JdkGf128;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * GF(2^128) using pure Java. The implementation comes from:
 * <p>org.bouncycastle.crypto.modes.gcm.GCMUtil.java</p>
 * <p></p>
 * Here we modify codes to reduce copying operations.
 *
 * @author Weiran Liu
 * @date 2023/8/27
 */
public class JdkGf2k extends AbstractGf2k {
    /**
     * JDK GF(2^128)
     */
    private final JdkGf128 jdkGf128;

    public JdkGf2k(EnvType envType) {
        super(envType);
        jdkGf128 = new JdkGf128(envType);
    }

    @Override
    public Gf2eType getGf2eType() {
        return jdkGf128.getGf2eType();
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.JDK;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return jdkGf128.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        jdkGf128.muli(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return jdkGf128.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        jdkGf128.invi(p);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return jdkGf128.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        jdkGf128.divi(p, q);
    }
}

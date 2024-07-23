package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.CombinedGf128;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * combined GF(2^128).
 *
 * @author Weiran Liu
 * @date 2023/7/2
 */
class CombinedGf2k extends AbstractGf2k {
    /**
     * Combined GF(2^128).
     */
    private final CombinedGf128 combinedGf128;

    CombinedGf2k(EnvType envType) {
        super(envType);
        combinedGf128 = new CombinedGf128(envType);
    }

    @Override
    public Gf2eType getGf2eType() {
        return combinedGf128.getGf2eType();
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.COMBINED;
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return combinedGf128.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        combinedGf128.divi(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return combinedGf128.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        combinedGf128.invi(p);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return combinedGf128.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        combinedGf128.muli(p, q);
    }
}

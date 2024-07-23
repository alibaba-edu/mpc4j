package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.RingsGf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;

/**
 * Rings GF(2^κ).
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
class RingsGf2k extends AbstractGf2k {
    /**
     * Rings GF(2^128)
     */
    private final RingsGf2e ringsGf128;

    RingsGf2k(EnvType envType) {
        super(envType);
        ringsGf128 = new RingsGf2e(envType, 128);
    }

    @Override
    public Gf2eType getGf2eType() {
        return ringsGf128.getGf2eType();
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.RINGS;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return ringsGf128.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        ringsGf128.muli(p, q);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return ringsGf128.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        ringsGf128.divi(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return ringsGf128.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        ringsGf128.invi(p);
    }
}

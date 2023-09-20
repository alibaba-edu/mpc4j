package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.crypto.modes.gcm.GCMUtil;

/**
 * 用Bouncy Castle实现的GF(2^128)。Java层的GCM运算为小端表示，因此需要转换大小端。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
class BcGf2k extends AbstractGf2k {

    BcGf2k(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.BC;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[] r = BytesUtils.reverseBitArray(p);
        GCMUtil.multiply(r, BytesUtils.reverseBitArray(q));
        BytesUtils.innerReverseBitArray(r);

        return r;
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        // here we must copy q, since we need to support muli(p, p).
        byte[] copyQ = BytesUtils.reverseBitArray(q);
        BytesUtils.innerReverseBitArray(p);
        GCMUtil.multiply(p, copyQ);
        BytesUtils.innerReverseBitArray(p);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        byte[] qInv = inv(q);
        return mul(p, qInv);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        byte[] qInv = inv(q);
        muli(p, qInv);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        // calculate p^{-1} as p^{2^{128}-2}. The addition chain below requires 142 mul/sqr operations total.
        byte[] a = BytesUtils.reverseBitArray(p);
        byte[] r = new byte[BYTE_L];
        for (int i = 0; i <= 6; i++) {
            // entering the loop a = p^{2^{2^i}-1}
            byte[] b = BytesUtils.clone(a);
            for (int j = 0; j < (1 << i); j++) {
                byte[] copyB = BytesUtils.clone(b);
                GCMUtil.multiply(b, copyB);
            }
            // after the loop b = a^{2^i} = p^{2^{2^i}*(2^{2^i}-1)}
            GCMUtil.multiply(a, b);
            // now a = x^{2^{2^{i+1}}-1}
            if (i == 0) {
                r = BytesUtils.clone(b);
            } else {
                GCMUtil.multiply(r, b);
            }
        }
        BytesUtils.innerReverseBitArray(r);
        return r;
    }

    @Override
    public void invi(byte[] p) {
        byte[] y = inv(p);
        System.arraycopy(y, 0, p, 0, BYTE_L);
    }
}

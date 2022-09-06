package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.crypto.modes.gcm.GCMUtil;

/**
 * 用Bouncy Castle实现的GF(2^128)。Java层的GCM运算为小端表示，因此需要转换大小端。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
class BcGf2k implements Gf2k {

    BcGf2k() {
        // empty
    }

    @Override
    public Gf2kType getGf2kType() {
        return Gf2kType.BC;
    }

    @Override
    public byte[] mul(byte[] a, byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        byte[] c = BytesUtils.reverseBitArray(a);
        GCMUtil.multiply(c, BytesUtils.reverseBitArray(b));
        BytesUtils.innerReverseBitArray(c);
        return c;
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        BytesUtils.innerReverseBitArray(a);
        GCMUtil.multiply(a, BytesUtils.reverseBitArray(b));
        BytesUtils.innerReverseBitArray(a);
    }
}

package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

/**
 * 本地NTL库GF(2^128)运算。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public class NtlGf2k implements Gf2k {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    NtlGf2k() {
        // empty
    }

    @Override
    public Gf2kFactory.Gf2kType getGf2kType() {
        return Gf2kFactory.Gf2kType.NTL;
    }

    @Override
    public byte[] mul(byte[] a, byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return nativeMul(a, b);
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        nativeMuli(a, b);
    }

    private native byte[] nativeMul(byte[] a, byte[] b);

    private native void nativeMuli(byte[] a, byte[] b);
}

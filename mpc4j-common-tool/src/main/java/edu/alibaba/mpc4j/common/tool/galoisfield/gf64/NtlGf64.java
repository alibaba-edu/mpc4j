package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;

/**
 * NTL GF(2^64).
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
class NtlGf64 extends AbstractGf64 {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    NtlGf64(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf64Type getGf64Type() {
        return Gf64Type.NTL;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        return nativeMul(p, q);
    }

    private native byte[] nativeMul(byte[] p, byte[] q);

    @Override
    public void muli(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        nativeMuli(p, q);
    }

    private native void nativeMuli(byte[] p, byte[] q);

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        return nativeInv(p);
    }

    private native byte[] nativeInv(byte[] p);

    @Override
    public void invi(byte[] p) {
        assert validateNonZeroElement(p);
        nativeInvi(p);
    }

    private native void nativeInvi(byte[] p);

    @Override
    public byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateNonZeroElement(q);
        return nativeDiv(p, q);
    }

    private native byte[] nativeDiv(byte[] p, byte[] q);

    @Override
    public void divi(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateNonZeroElement(q);
        nativeDivi(p, q);
    }

    private native void nativeDivi(byte[] p, byte[] q);
}

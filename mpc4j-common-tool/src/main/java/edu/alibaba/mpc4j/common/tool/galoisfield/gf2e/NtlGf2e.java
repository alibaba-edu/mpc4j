package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;

/**
 * GF(2^l) using NTL.
 *
 * @author Weiran Liu
 * @date 2022/5/18
 */
public class NtlGf2e extends AbstractGf2e {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    public NtlGf2e(EnvType envType, int l) {
        super(envType, l);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.NTL;
    }

    @Override
    public byte[] mul(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        return NtlNativeGf2e.nativeMul(minimalPolynomial, byteL, a, b);
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        NtlNativeGf2e.nativeMuli(minimalPolynomial, byteL, a, b);
    }

    @Override
    public byte[] div(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        return NtlNativeGf2e.nativeDiv(minimalPolynomial, byteL, a, b);
    }

    @Override
    public void divi(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        NtlNativeGf2e.nativeDivi(minimalPolynomial, byteL, a, b);
    }

    @Override
    public byte[] inv(byte[] a) {
        assert validateNonZeroElement(a);
        return NtlNativeGf2e.nativeInv(minimalPolynomial, byteL, a);
    }

    @Override
    public void invi(byte[] a) {
        assert validateNonZeroElement(a);
        NtlNativeGf2e.nativeInvi(minimalPolynomial, byteL, a);
    }
}

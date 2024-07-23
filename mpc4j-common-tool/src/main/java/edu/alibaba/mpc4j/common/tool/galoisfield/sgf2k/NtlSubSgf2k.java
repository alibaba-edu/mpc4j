package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * NTL Subfield GF2K for l ∈ {2, 4, 8, 16, 32, 64}.
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
public class NtlSubSgf2k extends AbstractSubSgf2k {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    public NtlSubSgf2k(EnvType envType, int subfieldL) {
        super(envType, subfieldL);
        nativeInit(subfieldL, subfieldMinimalPolynomial, fieldMinimalPolynomial);
    }

    private native void nativeInit(int subfieldL, byte[] subfieldMinimalPolynomial, byte[][] fieldMinimalPolynomial);

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[][] subfieldPs = decomposite(p);
        byte[][] subfieldQs = decomposite(q);
        return composite(nativeFieldMul(subfieldL, subfieldPs, subfieldQs));
    }

    private native byte[][] nativeFieldMul(int subfieldL, byte[][] subfieldPs, byte[][] subfieldQs);

    @Override
    public void muli(byte[] p, byte[] q) {
        byte[] result = mul(p, q);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateElement(p);
        byte[][] subfieldPs = decomposite(p);
        return composite(nativeFieldInv(subfieldL, subfieldPs));
    }

    private native byte[][] nativeFieldInv(int subfieldL, byte[][] subfieldPs);

    @Override
    public void invi(byte[] p) {
        byte[] result = inv(p);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[][] subfieldPs = decomposite(p);
        byte[][] subfieldQs = decomposite(q);
        return composite(nativeFieldDiv(subfieldL, subfieldPs, subfieldQs));
    }

    private native byte[][] nativeFieldDiv(int subfieldL, byte[][] subfieldPs, byte[][] subfieldQs);

    @Override
    public void divi(byte[] p, byte[] q) {
        byte[] result = div(p, q);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }
}

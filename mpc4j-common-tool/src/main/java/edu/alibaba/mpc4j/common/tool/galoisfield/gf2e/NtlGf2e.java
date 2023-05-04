package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 本地NTL库GF(2^e)运算。
 *
 * @author Weiran Liu
 * @date 2022/5/18
 */
public class NtlGf2e extends AbstractGf2e {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * GF(2^l)不可约多项式
     */
    private final byte[] minBytes;

    NtlGf2e(EnvType envType, int l) {
        super(envType, l);
        // 设置不可约多项式，系数个数为l + 1
        int minNum = l + 1;
        int minByteNum = CommonUtils.getByteLength(minNum);
        int minRoundBytes = minByteNum * Byte.SIZE;
        minBytes = new byte[minByteNum];
        UnivariatePolynomialZp64 minimalPolynomial = finiteField.getMinimalPolynomial();
        for (int i = 0; i <= minimalPolynomial.degree(); i++) {
            boolean coefficient = minimalPolynomial.get(i) != 0L;
            BinaryUtils.setBoolean(minBytes, minRoundBytes - 1 - i, coefficient);
        }
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.NTL;
    }

    @Override
    public byte[] mul(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        if (l == CommonConstants.BLOCK_BIT_LENGTH) {
            return gf2k.mul(a, b);
        } else {
            return NtlNativeGf2e.nativeMul(minBytes, byteL, a, b);
        }
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        if (l == CommonConstants.BLOCK_BIT_LENGTH) {
            gf2k.muli(a, b);
        } else {
            NtlNativeGf2e.nativeMuli(minBytes, byteL, a, b);
        }
    }

    @Override
    public byte[] div(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        return NtlNativeGf2e.nativeDiv(minBytes, byteL, a, b);
    }

    @Override
    public void divi(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        NtlNativeGf2e.nativeDivi(minBytes, byteL, a, b);
    }

    @Override
    public byte[] inv(byte[] a) {
        assert validateNonZeroElement(a);
        return NtlNativeGf2e.nativeInv(minBytes, byteL, a);
    }

    @Override
    public void invi(byte[] a) {
        assert validateNonZeroElement(a);
        NtlNativeGf2e.nativeInvi(minBytes, byteL, a);
    }
}

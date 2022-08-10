package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

/**
 * NTL的GF(2^l)有限域运算本地函数。
 *
 * @author Weiran Liu
 * @date 2022/5/18
 */
class NtlNativeGf2e {

    private NtlNativeGf2e() {
        // empty
    }

    /**
     * 计算a + b。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param a        元素a。
     * @param b        元素b。
     * @return a * b。
     */
    static native byte[] nativeMul(byte[] minBytes, int byteL, byte[] a, byte[] b);

    /**
     * 计算a + b，结果赋值到a中。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL l字节长度。
     * @param a 元素a。
     * @param b 元素b。
     */
    static native void nativeMuli(byte[] minBytes, int byteL, byte[] a, byte[] b);

    /**
     * 计算a / b。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param a        元素a。
     * @param b        元素b。
     */
    static native byte[] nativeDiv(byte[] minBytes, int byteL, byte[] a, byte[] b);

    /**
     * 计算a / b，结果赋值到a中。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param a        元素a。
     * @param b        元素b。
     */
    static native void nativeDivi(byte[] minBytes, int byteL, byte[] a, byte[] b);

    /**
     * 计算1 / a。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL l字节长度。
     * @param a 元素a。
     * @return a的逆。
     */
    static native byte[] nativeInv(byte[] minBytes, int byteL, byte[] a);

    /**
     * 计算1 / a，结果赋值到a中。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL l字节长度。
     * @param a 元素a。
     */
    static native void nativeInvi(byte[] minBytes, int byteL, byte[] a);
}

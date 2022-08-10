package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import java.nio.ByteBuffer;

/**
 * MCL实现SecP256r1椭圆曲线运算的本地函数。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
class MclNativeSecP256r1Ecc {
    /**
     * 单例模式
     */
    private static final MclNativeSecP256r1Ecc INSTANCE = new MclNativeSecP256r1Ecc();

    /**
     * 单例模式
     */
    private MclNativeSecP256r1Ecc() {
        init();
    }

    /**
     * 初始化本地的MCL椭圆曲线。
     */
    private synchronized native void init();

    static MclNativeSecP256r1Ecc getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化固定点乘法。
     *
     * @param pointString 用String表示的椭圆曲线点。
     * @return 固定点乘法指针。
     */
    synchronized native ByteBuffer precompute(String pointString);

    /**
     * 释放固定点乘法指针。
     *
     * @param windowHandler 固定点乘法指针。
     */
    synchronized native void destroyPrecompute(ByteBuffer windowHandler);

    /**
     * 固定点乘法。
     *
     * @param windowHandler 固定点乘法指针。
     * @param rString       幂指数。
     * @return 用String表示的MCL固定点乘法结果。
     */
    native String singleFixedPointMultiply(ByteBuffer windowHandler, String rString);

    /**
     * 固定点乘法。
     *
     * @param windowHandler 固定点乘法指针。
     * @param rStrings      幂指数数组。
     * @return 用String[]表示的MCL固定点乘法结果。
     */
    native String[] fixedPointMultiply(ByteBuffer windowHandler, String[] rStrings);

    /**
     * 椭圆曲线点乘。
     *
     * @param pointString 用String表示的椭圆曲线点。
     * @param rString     幂指数。
     * @return 用String表示的MCL乘法结果。
     */
    native String singleMultiply(String pointString, String rString);

    /**
     * 椭圆曲线点乘。
     *
     * @param pointString 椭圆曲线点。
     * @param rStrings    幂指数数组。
     * @return 用String[]表示的MCL乘法结果。
     */
    native String[] multiply(String pointString, String[] rStrings);
}

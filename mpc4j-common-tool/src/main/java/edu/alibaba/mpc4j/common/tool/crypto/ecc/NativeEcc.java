package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import java.nio.ByteBuffer;

/**
 * 本地Ecc接口。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public interface NativeEcc {
    /**
     * 初始化本地函数。
     */
    void init();

    /**
     * 初始化固定点乘法。
     *
     * @param pointString 用String表示的椭圆曲线点。
     * @return 固定点乘法指针。
     */
    ByteBuffer precompute(String pointString);

    /**
     * 释放固定点乘法指针。
     *
     * @param windowHandler 固定点乘法指针。
     */
    void destroyPrecompute(ByteBuffer windowHandler);

    /**
     * 固定点乘法。
     *
     * @param windowHandler 固定点乘法指针。
     * @param rString       幂指数。
     * @return 用String表示的MCL固定点乘法结果。
     */
    String singleFixedPointMultiply(ByteBuffer windowHandler, String rString);

    /**
     * 固定点乘法。
     *
     * @param windowHandler 固定点乘法指针。
     * @param rStrings      幂指数数组。
     * @return 用String[]表示的MCL固定点乘法结果。
     */
    String[] fixedPointMultiply(ByteBuffer windowHandler, String[] rStrings);

    /**
     * 椭圆曲线点乘。
     *
     * @param pointString 用String表示的椭圆曲线点。
     * @param rString     幂指数。
     * @return 用String表示的MCL乘法结果。
     */
    String singleMultiply(String pointString, String rString);

    /**
     * 椭圆曲线点乘。
     *
     * @param pointString 椭圆曲线点。
     * @param rStrings    幂指数数组。
     * @return 用String[]表示的MCL乘法结果。
     */
    String[] multiply(String pointString, String[] rStrings);

    /**
     * 重置。
     */
    void reset();
}

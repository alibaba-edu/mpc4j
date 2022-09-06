package edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.NativeEcc;

import java.nio.ByteBuffer;

/**
 * MCL实现SecP256k1椭圆曲线运算的本地函数。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
public class SecP256k1MclNativeEcc implements NativeEcc {
    /**
     * 单例模式
     */
    private static final SecP256k1MclNativeEcc INSTANCE = new SecP256k1MclNativeEcc();

    /**
     * 单例模式
     */
    private SecP256k1MclNativeEcc() {
        // empty
    }

    public static SecP256k1MclNativeEcc getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized native void init();

    @Override
    public synchronized native ByteBuffer precompute(String pointString);

    @Override
    public synchronized native void destroyPrecompute(ByteBuffer windowHandler);

    @Override
    public native String singleFixedPointMultiply(ByteBuffer windowHandler, String rString);

    @Override
    public native String[] fixedPointMultiply(ByteBuffer windowHandler, String[] rStrings);

    @Override
    public native String singleMultiply(String pointString, String rString);

    @Override
    public native String[] multiply(String pointString, String[] rStrings);

    @Override
    public synchronized native void reset();
}

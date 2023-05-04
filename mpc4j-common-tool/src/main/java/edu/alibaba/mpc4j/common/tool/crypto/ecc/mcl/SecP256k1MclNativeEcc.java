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
        init();
    }

    public static SecP256k1MclNativeEcc getInstance() {
        return INSTANCE;
    }

    @Override
    public native void init();

    @Override
    public native ByteBuffer precompute(String pointString);

    @Override
    public native void destroyPrecompute(ByteBuffer windowHandler);

    @Override
    public native String precomputeMultiply(ByteBuffer windowHandler, String rString);

    @Override
    public native String multiply(String pointString, String rString);

    @Override
    public native void reset();
}

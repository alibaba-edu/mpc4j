package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.NativeEcc;

import java.nio.ByteBuffer;

/**
 * OpenSSL实现SecP256r1椭圆曲线运算的本地函数。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public class Sm2P256v1OpensslNativeEcc implements NativeEcc {
    /**
     * 单例模式
     */
    private static final Sm2P256v1OpensslNativeEcc INSTANCE = new Sm2P256v1OpensslNativeEcc();

    /**
     * 单例模式
     */
    private Sm2P256v1OpensslNativeEcc() {
        init();
    }

    public static Sm2P256v1OpensslNativeEcc getInstance() {
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

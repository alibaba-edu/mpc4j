package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.NativeEcc;

import java.nio.ByteBuffer;

/**
 * OpenSSL实现SecP256r1椭圆曲线运算的本地函数。
 *
 * @author Weiran Liu
 * @date 2022/9/2
 */
public class SecP256r1OpensslNativeEcc implements NativeEcc {
    /**
     * 单例模式
     */
    private static final SecP256r1OpensslNativeEcc INSTANCE = new SecP256r1OpensslNativeEcc();

    /**
     * 单例模式
     */
    private SecP256r1OpensslNativeEcc() {
        init();
    }

    public static SecP256r1OpensslNativeEcc getInstance() {
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

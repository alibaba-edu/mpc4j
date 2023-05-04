package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地Ecc实现抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public abstract class AbstractNativeEcc extends AbstractEcc implements AutoCloseable {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 序列化为16进制
     */
    protected static final int RADIX = 16;
    /**
     * 本地MCL椭圆曲线服务单例
     */
    private final NativeEcc nativeEcc;
    /**
     * 预计算窗口映射
     */
    private final Map<ECPoint, ByteBuffer> windowHandlerMap;

    public AbstractNativeEcc(NativeEcc nativeEcc, EccFactory.EccType eccType, String bcCurveName) {
        super(eccType, bcCurveName);
        // 初始化窗口指针映射表
        windowHandlerMap = new HashMap<>();
        this.nativeEcc = nativeEcc;
    }

    @Override
    public void precompute(ECPoint p) {
        // 预计算的时间很长，因此要先判断给定点是否已经进行了预计算，如果没有，再执行预计算操作
        if (!windowHandlerMap.containsKey(p)) {
            ByteBuffer windowHandler = nativeEcc.precompute(ecPointToNativePointString(p));
            windowHandlerMap.put(p, windowHandler);
        }
    }

    @Override
    public void destroyPrecompute(ECPoint p) {
        if (windowHandlerMap.containsKey(p)) {
            ByteBuffer windowHandler = windowHandlerMap.get(p);
            nativeEcc.destroyPrecompute(windowHandler);
            windowHandlerMap.remove(p);
        }
    }

    @Override
    public ECPoint multiply(ECPoint p, BigInteger r) {
        String rString = r.toString(RADIX);
        if (windowHandlerMap.containsKey(p)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            ByteBuffer windowHandler = windowHandlerMap.get(p);
            String mulPointString = nativeEcc.precomputeMultiply(windowHandler, rString);
            return nativePointStringToEcPoint(mulPointString);
        } else {
            String pointString = ecPointToNativePointString(p);
            String mulPointString = nativeEcc.multiply(pointString, rString);
            return nativePointStringToEcPoint(mulPointString);
        }
    }

    /**
     * 将本地点的字符串转换为椭圆曲线点。
     *
     * @param nativePointString 点字符串。
     * @return 椭圆曲线点。
     */
    protected abstract ECPoint nativePointStringToEcPoint(String nativePointString);

    /**
     * 将椭圆曲线点转换为本地点的字符串。
     *
     * @param ecPoint 椭圆曲线点。
     * @return 本地点的字符串。
     */
    protected abstract String ecPointToNativePointString(ECPoint ecPoint);

    @Override
    public void close() {
        for (ByteBuffer windowHandler : windowHandlerMap.values()) {
            nativeEcc.destroyPrecompute(windowHandler);
        }
        windowHandlerMap.clear();
        nativeEcc.reset();
    }
}

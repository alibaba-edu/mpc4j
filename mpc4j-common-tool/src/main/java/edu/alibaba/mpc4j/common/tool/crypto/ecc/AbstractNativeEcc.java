package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl.SecP256k1MclNativeEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl.SecP256r1MclNativeEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl.SecP256k1OpensslNativeEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl.Sm2P256v1OpensslNativeEcc;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地Ecc实现抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public abstract class AbstractNativeEcc extends AbstractEcc implements AutoCloseable {
    /**
     * 当前椭圆曲线类型
     */
    static EccFactory.EccType currentEccType;

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
        if (currentEccType == null) {
            // 设置当前类型并初始化
            nativeEcc.init();
            currentEccType = eccType;
        } else if (!currentEccType.equals(eccType)) {
            // 如果当前本地类型不为我们的类型，则重置
            switch (currentEccType) {
                case SM2_P256_V1_OPENSSL:
                    Sm2P256v1OpensslNativeEcc.getInstance().reset();
                    break;
                case SEC_P256_K1_OPENSSL:
                    SecP256k1OpensslNativeEcc.getInstance().reset();
                    break;
                case SEC_P256_K1_MCL:
                    SecP256k1MclNativeEcc.getInstance().reset();
                    break;
                case SEC_P256_R1_MCL:
                    SecP256r1MclNativeEcc.getInstance().reset();
                    break;
                default:
                    break;
            }
            // 设置当前类型并初始化
            nativeEcc.init();
            currentEccType = eccType;
        }
    }

    @Override
    public void precompute(ECPoint ecPoint) {
        // 预计算的时间很长，因此要先判断给定点是否已经进行了预计算，如果没有，再执行预计算操作
        if (!windowHandlerMap.containsKey(ecPoint)) {
            ByteBuffer windowHandler = nativeEcc.precompute(ecPointToNativePointString(ecPoint));
            windowHandlerMap.put(ecPoint, windowHandler);
        }
    }

    @Override
    public void destroyPrecompute(ECPoint point) {
        if (windowHandlerMap.containsKey(point)) {
            ByteBuffer windowHandler = windowHandlerMap.get(point);
            nativeEcc.destroyPrecompute(windowHandler);
            windowHandlerMap.remove(point);
        }
    }

    @Override
    public ECPoint multiply(ECPoint ecPoint, BigInteger r) {
        String rString = r.toString(RADIX);
        if (windowHandlerMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            ByteBuffer windowHandler = windowHandlerMap.get(ecPoint);
            String mulPointString = nativeEcc.singleFixedPointMultiply(windowHandler, rString);
            return nativePointStringToEcPoint(mulPointString);
        } else {
            String pointString = ecPointToNativePointString(ecPoint);
            String mulPointString = nativeEcc.singleMultiply(pointString, rString);
            return nativePointStringToEcPoint(mulPointString);
        }
    }

    @Override
    public ECPoint[] multiply(ECPoint ecPoint, BigInteger[] rs) {
        assert rs.length > 0;
        String[] rStrings = Arrays.stream(rs).map(value -> value.toString(RADIX)).toArray(String[]::new);
        if (windowHandlerMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            ByteBuffer windowHandler = windowHandlerMap.get(ecPoint);
            String[] mulPointStrings = nativeEcc.fixedPointMultiply(windowHandler, rStrings);
            return Arrays.stream(mulPointStrings).map(this::nativePointStringToEcPoint).toArray(ECPoint[]::new);
        } else {
            String pointString = ecPointToNativePointString(ecPoint);
            String[] mulPointStrings = nativeEcc.multiply(pointString, rStrings);
            return Arrays.stream(mulPointStrings).map(this::nativePointStringToEcPoint).toArray(ECPoint[]::new);
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
    }
}

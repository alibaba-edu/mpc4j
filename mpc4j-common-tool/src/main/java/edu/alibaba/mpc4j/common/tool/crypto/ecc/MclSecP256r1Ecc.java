package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用MCL库实现的SecP256r1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class MclSecP256r1Ecc implements Ecc, AutoCloseable {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * MCL表示无穷远点的字符串
     */
    private static final String MCL_INFINITY_STRING = "0";
    /**
     * MCL序列化为16进制
     */
    private static final int MCL_STR_RADIX = 16;
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;
    /**
     * 本地MCL椭圆曲线服务单例
     */
    private final MclNativeSecP256r1Ecc mclNativeSecP256r1Ecc;
    /**
     * 大部分计直接放在Java层会比较快，因此在Java层存储一个对应的椭圆曲线参数
     */
    private final ECDomainParameters ecDomainParameters;
    /**
     * 预计算窗口映射
     */
    private final Map<ECPoint, ByteBuffer> windowHandlerMap;

    MclSecP256r1Ecc() {
        // 得到Bouncy Castle专门实现的优化SecP256r1曲线，转换成标准的椭圆曲线群参数
        X9ECParameters ecParameters = CustomNamedCurves.getByName("secp256r1");
        ECParameterSpec ecParameterSpec = new ECParameterSpec(ecParameters.getCurve(), ecParameters.getG(),
            ecParameters.getN(), ecParameters.getH(), ecParameters.getSeed());
        ecDomainParameters = new ECDomainParameters(
            ecParameterSpec.getCurve(), ecParameterSpec.getG(), ecParameterSpec.getN()
        );
        // 初始化窗口指针映射表
        windowHandlerMap = new HashMap<>();
        // 得到本地MCL椭圆曲线服务的单例
        mclNativeSecP256r1Ecc = MclNativeSecP256r1Ecc.getInstance();
        // 初始化哈希函数，为与MCL兼容，必须使用SHA256
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECDomainParameters getEcDomainParameters() {
        return this.ecDomainParameters;
    }

    @Override
    public ECPoint randomPoint(SecureRandom secureRandom) {
        while (true) {
            ECFieldElement x = ecDomainParameters.getCurve().randomFieldElement(secureRandom);
            ECFieldElement y = x.square()
                .add(ecDomainParameters.getCurve().getA()).multiply(x)
                .add(ecDomainParameters.getCurve().getB())
                .sqrt();
            if (y == null) {
                continue;
            }
            ECPoint ecPoint = ecDomainParameters.getCurve().createPoint(x.toBigInteger(), y.toBigInteger());
            // clearing the cofactor
            ecPoint = ecPoint.multiply(getCofactor());
            if (ecPoint == null || !ecPoint.isValid()) {
                continue;
            }
            return ecPoint;
        }
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        // 为何保证哈希结果的一致性，hashToCurve也在Java层实现
        byte[] messageHashBytes = hash.digestToBytes(message);
        while (true) {
            // 计算输入消息的哈希值，尝试构建坐标x和坐标y，如果失败，则继续哈希
            ECFieldElement x, y;
            // 哈希结果不需要模n，交给ECFieldElement判断结果是否合法
            BigInteger messageHash = BigIntegerUtils.byteArrayToNonNegBigInteger(messageHashBytes);
            try {
                x = ecDomainParameters.getCurve().fromBigInteger(messageHash);
            } catch (IllegalArgumentException e) {
                // 如果无法将哈希结果转换为坐标x，意味着哈希结果不是有效的椭圆曲线点，重新哈希
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            y = x.square()
                .add(ecDomainParameters.getCurve().getA()).multiply(x)
                .add(ecDomainParameters.getCurve().getB())
                .sqrt();
            if (y == null) {
                // 如果y无解，重新哈希
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            ECPoint ecPoint = ecDomainParameters.getCurve().createPoint(x.toBigInteger(), y.toBigInteger());
            // clearing the cofactor
            ecPoint = ecPoint.multiply(getCofactor());
            if (ecPoint == null || !ecPoint.isValid()) {
                messageHashBytes = hash.digestToBytes(messageHashBytes);
                continue;
            }
            return ecPoint;
        }
    }

    @Override
    public void precompute(ECPoint ecPoint) {
        // 预计算的时间很长，因此要先判断给定点是否已经进行了预计算，如果没有，再执行预计算操作
        if (!windowHandlerMap.containsKey(ecPoint)) {
            ByteBuffer windowHandler = mclNativeSecP256r1Ecc.precompute(ecPointToMclString(ecPoint));
            windowHandlerMap.put(ecPoint, windowHandler);
        }
    }

    @Override
    public void destroyPrecompute(ECPoint point) {
        if (windowHandlerMap.containsKey(point)) {
            ByteBuffer windowHandler = windowHandlerMap.get(point);
            mclNativeSecP256r1Ecc.destroyPrecompute(windowHandler);
            windowHandlerMap.remove(point);
        }
    }

    @Override
    public ECPoint multiply(ECPoint ecPoint, BigInteger r) {
        String rString = r.toString(MCL_STR_RADIX);
        if (windowHandlerMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            ByteBuffer windowHandler = windowHandlerMap.get(ecPoint);
            String mulPointString = mclNativeSecP256r1Ecc.singleFixedPointMultiply(windowHandler, rString);
            return mclStringToEcPoint(mulPointString);
        } else {
            String pointString = ecPointToMclString(ecPoint);
            String mulPointString = mclNativeSecP256r1Ecc.singleMultiply(pointString, rString);
            return mclStringToEcPoint(mulPointString);
        }
    }

    @Override
    public ECPoint[] multiply(ECPoint ecPoint, BigInteger[] rs) {
        assert rs.length > 0;
        String[] rStrings = Arrays.stream(rs).map(value -> value.toString(MCL_STR_RADIX)).toArray(String[]::new);
        if (windowHandlerMap.containsKey(ecPoint)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            ByteBuffer windowHandler = windowHandlerMap.get(ecPoint);
            String[] mulPointStrings = mclNativeSecP256r1Ecc.fixedPointMultiply(windowHandler, rStrings);
            return Arrays.stream(mulPointStrings).map(this::mclStringToEcPoint).toArray(ECPoint[]::new);
        } else {
            String pointString = ecPointToMclString(ecPoint);
            String[] mulPointStrings = mclNativeSecP256r1Ecc.multiply(pointString, rStrings);
            return Arrays.stream(mulPointStrings).map(this::mclStringToEcPoint).toArray(ECPoint[]::new);
        }
    }

    @Override
    public EccFactory.EccType getEccType() {
        return EccFactory.EccType.MCL_SEC_P256_R1;
    }

    /**
     * 将用String表示的MCL椭圆曲线点转化为用ECPoint表示的Bouncy Castle椭圆曲线点。
     *
     * @param mclString 用String表示的MCL椭圆曲线点。
     * @return 用ECPoint表示的Bouncy Castle椭圆曲线点。
     */
    private ECPoint mclStringToEcPoint(String mclString) {
        // 如果返回结果是0元，需要单独处理
        if (MCL_INFINITY_STRING.equals(mclString)) {
            return getInfinity();
        }
        // Bouncy Castle中的ECPoint不支持设置点的参数Z，因此C++层所有计算结果都需要normalize
        String[] mclStrings = mclString.split(" ");
        assert mclStrings.length == 3;
        return ecDomainParameters.getCurve().createPoint(
            new BigInteger(mclStrings[1], MCL_STR_RADIX), new BigInteger(mclStrings[2], MCL_STR_RADIX)
        );
    }

    /**
     * 将用ECPoint表示的椭圆曲线点转化为用String表示的MCL椭圆曲线点。
     *
     * @param ecPoint 用ECPoint表示的Bouncy Castle椭圆曲线点。
     * @return 用ECPoint表示的Bouncy Castle椭圆曲线点。
     */
    private String ecPointToMclString(ECPoint ecPoint) {
        // Bouncy Castle中的ECPoint可以得到点的参数Z，MCL椭圆曲线库的格式为"Z X Y"，必须要先归一化再完成计算
        ECPoint normalizedEcPoint = ecPoint.normalize();
        return normalizedEcPoint.getZCoord(0)
            + " " + normalizedEcPoint.getXCoord()
            + " " + normalizedEcPoint.getYCoord();
    }

    @Override
    public void close() {
        for (ByteBuffer windowHandler : this.windowHandlerMap.values()) {
            mclNativeSecP256r1Ecc.destroyPrecompute(windowHandler);
        }
    }
}

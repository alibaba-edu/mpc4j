package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.*;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Bouncy Castle椭圆曲线抽象类。其中，HashToPoint采用Google的private-join-and-compute方法实现，参见：
 * <p>
 * https://github.com/google/private-join-and-compute/blob/master/private_join_and_compute/crypto/ec_group.cc
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/05/30
 */
public abstract class AbstractEcc implements Ecc {
    /**
     * 窗口大小
     */
    private static final int WINDOW_SIZE = 16;
    /**
     * 椭圆曲线类型
     */
    protected final EccFactory.EccType eccType;
    /**
     * 椭圆曲线参数
     */
    protected final ECDomainParameters ecDomainParameters;
    /**
     * 预计算窗口映射
     */
    private final Map<ECPoint, WindowMethod> windowMethodMap;

    public AbstractEcc(EccFactory.EccType eccType, String bcCurveName) {
        X9ECParameters ecParameters = CustomNamedCurves.getByName(bcCurveName);
        ECParameterSpec ecParameterSpec = new ECParameterSpec(
            ecParameters.getCurve(), ecParameters.getG(), ecParameters.getN(),
            ecParameters.getH(), ecParameters.getSeed()
        );
        ecDomainParameters = new ECDomainParameters(
            ecParameterSpec.getCurve(), ecParameterSpec.getG(), ecParameterSpec.getN()
        );
        // 初始化窗口指针映射表
        windowMethodMap = new HashMap<>(0);
        this.eccType = eccType;
    }

    @Override
    public ECDomainParameters getEcDomainParameters() {
        return ecDomainParameters;
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

    protected ECPoint hashToCurve(byte[] data, Hash hash) {
        // 计算输入消息的哈希值，尝试构建坐标x和坐标y，如果失败，则继续哈希
        byte[] messageHashBytes = hash.digestToBytes(data);
        while (true) {
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
    public void precompute(ECPoint p) {
        if (!windowMethodMap.containsKey(p)) {
            // 先判断给定点是否已经进行了预计算，如果没有，再执行预计算操作
            WindowMethod windowMethod = new WindowMethod(this, p, WINDOW_SIZE);
            windowMethod.init();
            windowMethodMap.put(p, windowMethod);
        }
    }

    @Override
    public void destroyPrecompute(ECPoint p) {
        windowMethodMap.remove(p);
    }

    @Override
    public ECPoint multiply(ECPoint p, BigInteger r) {
        if (windowMethodMap.containsKey(p)) {
            // 先判断给定点是否已经进行了预计算，如果进行过预计算，则用预计算乘法处理
            WindowMethod windowMethod = windowMethodMap.get(p);
            return windowMethod.multiply(r);
        } else {
            return p.multiply(r);
        }
    }

    @Override
    public EccFactory.EccType getEccType() {
        return eccType;
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 椭圆曲线运算接口。
 *
 * @author Weiran Liu
 * @date 2021/05/23
 */
public interface Ecc {
    /**
     * 返回椭圆曲线群参数。
     *
     * @return 椭圆曲线群参数。
     */
    ECDomainParameters getEcDomainParameters();

    /**
     * 返回椭圆曲线的阶。
     *
     * @return 椭圆曲线的阶。
     */
    default BigInteger getN() {
        return getEcDomainParameters().getN();
    }

    /**
     * 返回椭圆曲线的辅因子。
     *
     * @return 辅因子。
     */
    default BigInteger getCofactor() {
        return getEcDomainParameters().getCurve().getCofactor();
    }

    /**
     * 返回1个随机幂指数。
     *
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    default BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(getN(), secureRandom);
    }

    /**
     * 返回{@code num}随机幂指数。
     *
     * @param num          随机幂指数数量。
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    default BigInteger[] randomZn(int num, SecureRandom secureRandom) {
        assert num > 0;
        return IntStream.range(0, num).mapToObj(index -> randomZn(secureRandom)).toArray(BigInteger[]::new);
    }

    /**
     * 返回椭圆曲线无穷远点。
     *
     * @return 无穷远点。
     */
    default ECPoint getInfinity() {
        return getEcDomainParameters().getCurve().getInfinity();
    }

    /**
     * 返回椭圆曲线的生成元。
     *
     * @return 椭圆曲线的生成元。
     */
    default ECPoint getG() {
        return getEcDomainParameters().getG();
    }

    /**
     * 返回1个随机的椭圆曲线点。
     *
     * @param secureRandom 随机状态。
     * @return 随机椭圆曲线点。
     */
    ECPoint randomPoint(SecureRandom secureRandom);

    /**
     * 将{@code byte[]}表示的数据映射到椭圆曲线上。
     *
     * @param message 数据。
     * @return 数据的椭圆曲线哈希结果。
     */
    ECPoint hashToCurve(byte[] message);

    /**
     * 编码椭圆曲线点。
     *
     * @param ecPoint 椭圆曲线点。
     * @return 编码结果。
     */
    default byte[] encode(ECPoint ecPoint, boolean compressed) {
        return ecPoint.getEncoded(compressed);
    }

    /**
     * 解码椭圆曲线点。
     *
     * @param encoded 编码椭圆曲线点。
     * @return 解码结果。
     */
    default ECPoint decode(byte[] encoded) {
        return getEcDomainParameters().getCurve().decodePoint(encoded);
    }

    /**
     * 预计算椭圆曲线点。
     *
     * @param ecPoint 椭圆曲线点。
     */
    void precompute(ECPoint ecPoint);

    /**
     * 移除预计算椭圆曲线点。
     *
     * @param ecPoint 椭圆曲线点。
     */
    void destroyPrecompute(ECPoint ecPoint);

    /**
     * 计算椭圆曲线点乘以幂指数。
     *
     * @param ecPoint 椭圆曲线点。
     * @param r       幂指数。
     * @return 乘法结果。
     */
    ECPoint multiply(ECPoint ecPoint, BigInteger r);

    /**
     * 计算一个椭圆曲线点与多个指数相乘。
     *
     * @param ecPoint 椭圆曲线点。
     * @param rs      幂指数数组。
     * @return 乘法结果。
     */
    ECPoint[] multiply(ECPoint ecPoint, BigInteger[] rs);

    /**
     * 计算输入椭圆曲线点数组的和。
     *
     * @param ecPoints 椭圆曲线点数组。
     * @return 椭圆曲线点数组的和。
     */
    default ECPoint add(ECPoint[] ecPoints) {
        assert ecPoints.length > 0;
        ECPoint output = getInfinity();
        for (ECPoint point : ecPoints) {
            output = output.add(point);
        }
        return output;
    }

    /**
     * 计算布尔向量和椭圆曲线点向量的内积。
     *
     * @param binary   布尔向量。
     * @param ecPoints 椭圆曲线点向量。
     * @return 内积结果。
     */
    default ECPoint innerProduct(boolean[] binary, ECPoint[] ecPoints) {
        assert binary.length > 0 && ecPoints.length > 0;
        assert binary.length == ecPoints.length;
        ECPoint innerProduct = getInfinity();
        for (int index = 0; index < ecPoints.length; index++) {
            if (binary[index]) {
                innerProduct = innerProduct.add(ecPoints[index]);
            }
        }
        return innerProduct;
    }

    /**
     * 返回椭圆曲线类型。
     *
     * @return 椭圆曲线类型。
     */
    EccFactory.EccType getEccType();
}

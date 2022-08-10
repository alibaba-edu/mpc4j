package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * 应用Bouncy Castle实现的Ed25519。底层使用Curve25519，编解码时使用Edwards曲线表示，即可得到ED25519的椭圆曲线表示。
 *
 * @author Liqiang Peng, Weiran Liu
 * @date 2022/5/20
 */
class BcEd25519Ecc extends AbstractBcEcc {
    /**
     * 坐标的字节长度
     */
    private static final int POINT_BYTES = 32;
    /**
     * 常数3
     */
    private final ECFieldElement ecFieldElement3;
    /**
     * 常数486662
     */
    private final ECFieldElement ecFieldElement486662;
    /**
     * 常数a
     */
    private final ECFieldElement aSqrt;
    /**
     * 常数1
     */
    private final ECFieldElement one;
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    BcEd25519Ecc() {
        super(EccFactory.EccType.BC_ED_25519, "curve25519");
        // 初始化哈希函数，为与MCL兼容，必须使用SHA256
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
        // 初始化常数
        ecFieldElement3 = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(3));
        ecFieldElement486662 = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(486662));
        aSqrt = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(486664)).negate().sqrt();
        one = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.ONE);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }

    @Override
    public byte[] encode(ECPoint ecPoint, boolean compressed) {
        // 转换成ed25519曲线上的点，输出坐标，ED25519无法简单的压缩表示。
        ECPoint normalizedPoint = ecPoint.normalize();
        ECFieldElement[] t1 = weierstrassToMontgomery(normalizedPoint.getAffineXCoord(), normalizedPoint.getAffineYCoord());
        ECFieldElement[] t2 = montgomeryToEdwards(t1[0], t1[1]);
        return ByteBuffer.allocate(POINT_BYTES * 2)
                .put(BigIntegerUtils.nonNegBigIntegerToByteArray(t2[0].negate().toBigInteger(), POINT_BYTES))
                .put(BigIntegerUtils.nonNegBigIntegerToByteArray(t2[1].toBigInteger(), POINT_BYTES))
                .array();
    }

    @Override
    public ECPoint decode(byte[] encoded) {
        assert encoded.length == POINT_BYTES * 2;
        byte[] encodeEdwardPoint = new byte[POINT_BYTES];
        ECFieldElement[] edwardsPoint = new ECFieldElement[2];
        // 转换x
        System.arraycopy(encoded, 0, encodeEdwardPoint, 0, POINT_BYTES);
        edwardsPoint[0] = getEcDomainParameters().getCurve().fromBigInteger(
                BigIntegerUtils.byteArrayToNonNegBigInteger(encodeEdwardPoint)
        ).negate();
        // 转换y
        System.arraycopy(encoded, POINT_BYTES, encodeEdwardPoint, 0, POINT_BYTES);
        edwardsPoint[1] = getEcDomainParameters().getCurve().fromBigInteger(
                BigIntegerUtils.byteArrayToNonNegBigInteger(encodeEdwardPoint)
        );
        // 转换为Weierstrass点
        ECFieldElement[] t2 = edwardsToMontgomery(edwardsPoint[0], edwardsPoint[1]);
        ECFieldElement[] t1 = montgomeryToWeierstrass(t2[0], t2[1]);

        return getEcDomainParameters().getCurve().createPoint(t1[0].toBigInteger(), t1[1].toBigInteger());
    }

    private ECFieldElement[] weierstrassToMontgomery(ECFieldElement x, ECFieldElement y) {
        ECFieldElement[] montgomeryPoint = new ECFieldElement[2];
        // u = (3x - 486662) / 3
        montgomeryPoint[0] = (ecFieldElement3.multiply(x).subtract(ecFieldElement486662)).divide(ecFieldElement3);
        // v = y
        montgomeryPoint[1] = y;
        return montgomeryPoint;
    }

    private ECFieldElement[] montgomeryToEdwards(ECFieldElement u, ECFieldElement v) {
        ECFieldElement[] edwardsPoint = new ECFieldElement[2];
        // x = √a * u / v
        edwardsPoint[0] = aSqrt.multiply(u).divide(v);
        // y = (u - 1) / (u + 1)
        edwardsPoint[1] = u.subtract(one).divide(u.addOne());
        return edwardsPoint;
    }

    private ECFieldElement[] edwardsToMontgomery(ECFieldElement x, ECFieldElement y) {
        ECFieldElement[] montgomeryPoint = new ECFieldElement[2];
        // u = (1 + y) / (1 - y)
        montgomeryPoint[0] = one.add(y).divide(one.subtract(y));
        // v = √a * u / x
        montgomeryPoint[1] = aSqrt.multiply(montgomeryPoint[0]).divide(x);
        return montgomeryPoint;
    }

    private ECFieldElement[] montgomeryToWeierstrass(ECFieldElement u, ECFieldElement v) {
        ECFieldElement[] weierstrassPoint = new ECFieldElement[2];
        // x = (3u + 486662) / 3
        weierstrassPoint[0] = (ecFieldElement3.multiply(u).add(ecFieldElement486662)).divide(ecFieldElement3);
        // v = y
        weierstrassPoint[1] = v;
        return weierstrassPoint;
    }
}

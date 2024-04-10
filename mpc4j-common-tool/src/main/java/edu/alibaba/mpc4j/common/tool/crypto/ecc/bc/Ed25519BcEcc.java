package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
public class Ed25519BcEcc extends AbstractEcc {
    /**
     * 坐标的字节长度
     */
    private static final int POINT_BYTES = 32;
    /**
     * 压缩坐标长度
     */
    private static final int COMPRESS_POINT_BYTES = POINT_BYTES + 1;
    /**
     * 非压缩坐标长度
     */
    private static final int UNCOMPRESS_POINT_BYTES = POINT_BYTES * 2 + 1;
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
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private Ed25519BcEcc() {
        super(EccFactory.EccType.ED25519_BC, "curve25519");
        // initialize the hash function with SHA256
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
        // 初始化常数
        ecFieldElement3 = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(3));
        ecFieldElement486662 = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(486662));
        aSqrt = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.valueOf(486664)).negate().sqrt();
        one = getEcDomainParameters().getCurve().fromBigInteger(BigInteger.ONE);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    @Override
    public byte[] encode(ECPoint p, boolean compressed) {
        // 转换成ed25519曲线上的点
        ECPoint normalizedPoint = p.normalize();
        ECFieldElement[] t1 = weierstrassToMontgomery(normalizedPoint.getAffineXCoord(), normalizedPoint.getAffineYCoord());
        ECFieldElement[] t2 = montgomeryToEdwards(t1[0], t1[1]);
        byte[] encodeY = BigIntegerUtils.nonNegBigIntegerToByteArray(t2[1].toBigInteger(), POINT_BYTES);
        if (compressed) {
            // 压缩表示，将编码结果修正为y坐标
            byte[] encoded = p.getEncoded(true);
            // 将尾数修改为Edwards形式
            System.arraycopy(encodeY, 0, encoded, 1, POINT_BYTES);
            return encoded;
        } else {
            // 非压缩表示，第1个字节为符号位0x04
            byte[] encodeX = BigIntegerUtils.nonNegBigIntegerToByteArray(t2[0].toBigInteger(), POINT_BYTES);
            return ByteBuffer.allocate(UNCOMPRESS_POINT_BYTES)
                .put((byte)0x04)
                .put(encodeX)
                .put(encodeY)
                .array();
        }
    }

    @Override
    public ECPoint decode(byte[] encoded) {
        assert encoded.length == COMPRESS_POINT_BYTES || encoded.length == UNCOMPRESS_POINT_BYTES
            : "encode byte length must be either " + COMPRESS_POINT_BYTES
            + " or " + UNCOMPRESS_POINT_BYTES + ": " + encoded.length;
        byte[] copyEncode = BytesUtils.clone(encoded);
        if (encoded.length == UNCOMPRESS_POINT_BYTES) {
            // 完整表示
            byte[] encodeEdwardPoint = new byte[POINT_BYTES];
            ECFieldElement[] edwardsPoint = new ECFieldElement[2];
            // 转换x
            System.arraycopy(encoded, 1, encodeEdwardPoint, 0, POINT_BYTES);
            edwardsPoint[0] = getEcDomainParameters().getCurve().fromBigInteger(
                BigIntegerUtils.byteArrayToNonNegBigInteger(encodeEdwardPoint)
            );
            // 转换y
            System.arraycopy(encoded, POINT_BYTES + 1, encodeEdwardPoint, 0, POINT_BYTES);
            edwardsPoint[1] = getEcDomainParameters().getCurve().fromBigInteger(
                BigIntegerUtils.byteArrayToNonNegBigInteger(encodeEdwardPoint)
            );
            // 转换为Weierstrass点
            ECFieldElement[] t2 = edwardsToMontgomery(edwardsPoint[0], edwardsPoint[1]);
            ECFieldElement[] t1 = montgomeryToWeierstrass(t2[0], t2[1]);
            byte[] encodeX = BigIntegerUtils.nonNegBigIntegerToByteArray(t1[0].toBigInteger(), POINT_BYTES);
            byte[] encodeY = BigIntegerUtils.nonNegBigIntegerToByteArray(t1[1].toBigInteger(), POINT_BYTES);
            System.arraycopy(encodeX, 0, copyEncode, 1, POINT_BYTES);
            System.arraycopy(encodeY, 0, copyEncode, POINT_BYTES + 1, POINT_BYTES);
        } else {
            // 压缩表示，将编码结果修正为x坐标
            byte[] encodeEdwardY = new byte[POINT_BYTES];
            System.arraycopy(encoded, 1, encodeEdwardY, 0, POINT_BYTES);
            ECFieldElement edwardsY = getEcDomainParameters().getCurve().fromBigInteger(
                BigIntegerUtils.byteArrayToNonNegBigInteger(encodeEdwardY)
            );
            ECFieldElement montgomeryX = one.add(edwardsY).divide(one.subtract(edwardsY));
            ECFieldElement weierstrassX = (ecFieldElement3.multiply(montgomeryX).add(ecFieldElement486662))
                .divide(ecFieldElement3);
            byte[] encodeX = BigIntegerUtils.nonNegBigIntegerToByteArray(weierstrassX.toBigInteger(), POINT_BYTES);
            System.arraycopy(encodeX, 0, copyEncode, 1, POINT_BYTES);
        }
        return getEcDomainParameters().getCurve().decodePoint(copyEncode);
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

    /**
     * singleton mode
     */
    private static final Ed25519BcEcc INSTANCE = new Ed25519BcEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Ed25519BcEcc getInstance() {
        return INSTANCE;
    }
}

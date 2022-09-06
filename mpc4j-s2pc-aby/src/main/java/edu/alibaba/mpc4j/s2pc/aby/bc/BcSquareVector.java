package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

/**
 * 布尔电路方括号向量（[x]），即布尔电路的布尔秘密分享值（Boolean Secret-Shared Value）。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class BcSquareVector implements BcVector {
    /**
     * 布尔秘密分享值
     */
    private byte[] bytes;
    /**
     * 比特长度
     */
    private int bitLength;
    /**
     * 是否为明文状态，布尔电路仅可能全部为明文或全部为密文
     */
    private boolean isPublic;

    /**
     * 构造指定取值不经意比特向量。从性能角度考虑，传入时不复制导线值。
     *
     * @param bytes     导线值。
     * @param bitLength 导线组长度。
     * @param isPublic  是否为公开导线组。
     * @return 指定取值不经意比特向量。
     */
    public static BcSquareVector create(byte[] bytes, int bitLength, boolean isPublic) {
        assert bitLength > 0;
        assert bytes.length == CommonUtils.getByteLength(bitLength);
        assert BytesUtils.isReduceByteArray(bytes, bitLength);
        BcSquareVector bcSquareVector = new BcSquareVector();
        bcSquareVector.bytes = bytes;
        bcSquareVector.bitLength = bitLength;
        bcSquareVector.isPublic = isPublic;

        return bcSquareVector;
    }

    /**
     * 构造全1取值不经意比特向量。
     *
     * @param bitLength 比特向量长度。
     * @return 全1取值不经意比特向量。
     */
    public static BcSquareVector createOnes(int bitLength) {
        assert bitLength > 0;
        int byteLength = CommonUtils.getByteLength(bitLength);
        // 创建全1导线并修正
        byte[] ones = new byte[byteLength];
        Arrays.fill(ones, (byte)0xFF);
        BytesUtils.reduceByteArray(ones, bitLength);
        // 构造返回值
        BcSquareVector onesBcWireGroup = new BcSquareVector();
        onesBcWireGroup.bytes = ones;
        onesBcWireGroup.bitLength = bitLength;
        onesBcWireGroup.isPublic = true;

        return onesBcWireGroup;
    }

    /**
     * 构造全0取值不经意比特向量。
     *
     * @param bitLength 比特向量长度。
     * @return 全0取值不经意比特向量。
     */
    public static BcSquareVector createZeros(int bitLength) {
        assert bitLength > 0;
        int byteLength = CommonUtils.getByteLength(bitLength);
        // 创建全0导线组，不需要修正
        byte[] zeros = new byte[byteLength];
        // 构造返回值
        BcSquareVector zerosWireGroup = new BcSquareVector();
        zerosWireGroup.bytes = zeros;
        zerosWireGroup.bitLength = bitLength;
        zerosWireGroup.isPublic = true;

        return zerosWireGroup;
    }

    /**
     * 复制一个不经意比特向量。
     *
     * @param bcWireGroup 原始不经意比特向量。
     * @return 复制不经意比特向量。
     */
    public static BcSquareVector clone(BcSquareVector bcWireGroup) {
        BcSquareVector clone = new BcSquareVector();
        clone.bytes = BytesUtils.clone(bcWireGroup.bytes);
        clone.bitLength = bcWireGroup.bitLength;
        clone.isPublic = bcWireGroup.isPublic;

        return clone;
    }

    private BcSquareVector() {
        // empty
    }

    @Override
    public int bitLength() {
        return bitLength;
    }

    @Override
    public int byteLength() {
        return bytes.length;
    }

    @Override
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * 返回不经意比特向量取值。
     *
     * @return 不经意比特向量取值。
     */
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bytes)
            .append(bitLength)
            .append(isPublic)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BcSquareVector) {
            BcSquareVector that = (BcSquareVector)obj;
            return new EqualsBuilder()
                .append(this.bytes, that.bytes)
                .append(this.bitLength, that.bitLength)
                .append(this.isPublic, that.isPublic)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format(
            "%s (%s bits): %s", isPublic ? "Public" : "Secret", bitLength, Hex.toHexString(bytes)
        );
    }
}

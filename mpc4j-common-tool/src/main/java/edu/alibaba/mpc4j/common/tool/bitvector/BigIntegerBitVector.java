package edu.alibaba.mpc4j.common.tool.bitvector;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.Random;

/**
 * The bit vector represented by BigInteger.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public class BigIntegerBitVector implements BitVector {
    /**
     * bit vector represented by BigInteger.
     */
    private BigInteger bigInteger;
    /**
     * number of bit.
     */
    private int bitNum;
    /**
     * number of byte.
     */
    private int byteNum;

    static BitVector create(int bitNum, byte[] bytes) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteNum = CommonUtils.getByteLength(bitNum);
        assert bytes.length == byteNum : "bytes.length must be equal to " + byteNum + ": " + bytes.length;
        assert BytesUtils.isReduceByteArray(bytes, bitNum) : "bytes must contain at most " + bitNum + " bits";
        // create instance
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(bytes);
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteNum;
        return bitVector;
    }

    static BitVector create(int bitNum, BigInteger bigInteger) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        assert BigIntegerUtils.greaterOrEqual(bigInteger, BigInteger.ZERO)
            : "bigInteger must be greater than or equal to 0: " + bigInteger;
        assert bigInteger.bitLength() <= bitNum
            : "bigInteger.bitLength must be less than or equal to " + bitNum + ": " + bigInteger.bitLength();
        int byteNum = CommonUtils.getByteLength(bitNum);
        // create instance
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = bigInteger;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteNum;
        return bitVector;
    }

    static BitVector createRandom(int bitNum, Random random) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        // create random BigInteger
        BigInteger bigInteger = new BigInteger(bitNum, random);
        int byteNum = CommonUtils.getByteLength(bitNum);
        // create instance
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = bigInteger;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteNum;
        return bitVector;
    }

    static BitVector createOnes(int bitNum) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create BigInteger with all 1
        BigInteger bigInteger = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE);
        // create instance
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = bigInteger;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        return bitVector;
    }

    static BitVector createZeros(int bitNum) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create instance
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = BigInteger.ZERO;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        return bitVector;
    }

    static BitVector createEmpty() {
        BigIntegerBitVector bitVector = new BigIntegerBitVector();
        bitVector.bigInteger = BigInteger.ZERO;
        bitVector.bitNum = 0;
        bitVector.byteNum = 0;
        return bitVector;
    }


    @Override
    public BitVectorFactory.BitVectorType getType() {
        return BitVectorFactory.BitVectorType.BIGINTEGER_BIT_VECTOR;
    }

    @Override
    public void set(int index, boolean value) {
        assert index >= 0 && index < bitNum : "index must be in range [0, " + bitNum + ")";
        if (value) {
            bigInteger = bigInteger.setBit(bitNum - 1 - index);
        } else {
            bigInteger = bigInteger.setBit(bitNum - 1 - index).flipBit(bitNum - 1 - index);
        }
    }

    @Override
    public boolean get(int index) {
        assert index >= 0 && index < bitNum : "index must be in range [0, " + bitNum + ")";
        return bigInteger.testBit(bitNum - 1 - index);
    }

    @Override
    public BitVector copy() {
        BigIntegerBitVector copyBitVector = new BigIntegerBitVector();
        // BigInteger is immutable, do not need to copy
        copyBitVector.bigInteger = bigInteger;
        copyBitVector.bitNum = bitNum;
        copyBitVector.byteNum = byteNum;

        return copyBitVector;
    }

    @Override
    public void replaceCopy(BitVector that) {
        assertEqualBitNum(that);
        // BigInteger is immutable, do not need to copy
        this.bigInteger = that.getBigInteger();
    }

    @Override
    public int bitNum() {
        return bitNum;
    }

    @Override
    public int byteNum() {
        return byteNum;
    }

    @Override
    public byte[] getBytes() {
        return BigIntegerUtils.nonNegBigIntegerToByteArray(bigInteger, byteNum);
    }

    @Override
    public BigInteger getBigInteger() {
        return bigInteger;
    }

    @Override
    public BitVector split(int bitNum) {
        assert bitNum > 0 && bitNum <= this.bitNum
            : "number of split bits must be in range (0, " + this.bitNum + "]: " + bitNum;
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(this.bitNum - bitNum).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger splitBigInteger = bigInteger.shiftRight(this.bitNum - bitNum);
        // update the remained bit vector
        bigInteger = bigInteger.and(mask);
        this.bitNum = this.bitNum - bitNum;
        byteNum = this.bitNum == 0 ? 0 : CommonUtils.getByteLength(this.bitNum);
        // return a new instance
        return BigIntegerBitVector.create(bitNum, splitBigInteger);
    }

    @Override
    public void reduce(int bitNum) {
        assert bitNum > 0 && bitNum <= this.bitNum
            : "number of reduced bits must be in range (0, " + this.bitNum + "]: " + bitNum;
        if (bitNum < this.bitNum) {
            // 缩减长度，方法为原始数据与长度对应全1比特串求AND
            BigInteger mask = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE);
            // update the remained bit vector
            bigInteger = bigInteger.and(mask);
            this.bitNum = bitNum;
            byteNum = CommonUtils.getByteLength(this.bitNum);
        }
    }

    @Override
    public void merge(BitVector that) {
        BigInteger mergeBigInteger = that.getBigInteger();
        // shift the remained bit vector
        bigInteger = bigInteger.shiftLeft(that.bitNum()).or(mergeBigInteger);
        bitNum += that.bitNum();
        byteNum = bitNum == 0 ? 0 : CommonUtils.getByteLength(bitNum);
    }

    @Override
    public BitVector xor(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BigIntegerBitVector.createEmpty();
        } else {
            return BigIntegerBitVector.create(bitNum, bigInteger.xor(that.getBigInteger()));
        }
    }

    @Override
    public void xori(BitVector that) {
        assertEqualBitNum(that);
        bigInteger = bigInteger.xor(that.getBigInteger());
    }

    @Override
    public BitVector and(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BigIntegerBitVector.createEmpty();
        } else {
            return BigIntegerBitVector.create(bitNum, bigInteger.and(that.getBigInteger()));
        }
    }

    @Override
    public void andi(BitVector that) {
        assertEqualBitNum(that);
        bigInteger = bigInteger.and(that.getBigInteger());
    }

    @Override
    public BitVector or(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BigIntegerBitVector.createEmpty();
        } else {
            return BigIntegerBitVector.create(bitNum, bigInteger.or(that.getBigInteger()));
        }
    }

    @Override
    public void ori(BitVector that) {
        assertEqualBitNum(that);
        bigInteger = bigInteger.or(that.getBigInteger());
    }

    @Override
    public BitVector not() {
        if (bitNum == 0) {
            return BigIntegerBitVector.createEmpty();
        } else {
            BigInteger notBigInteger = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE);
            return BigIntegerBitVector.create(bitNum, bigInteger.xor(notBigInteger));
        }
    }

    @Override
    public void noti() {
        BigInteger notBigInteger = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE);
        bigInteger = bigInteger.xor(notBigInteger);
    }

    private void assertEqualBitNum(BitVector that) {
        assert bitNum == that.bitNum() : "the given bit vector must contain " + bitNum + " bits: " + that.bitNum();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bigInteger)
            .append(bitNum)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BigIntegerBitVector) {
            BigIntegerBitVector that = (BigIntegerBitVector) obj;
            return new EqualsBuilder()
                .append(this.bigInteger, that.bigInteger)
                .append(this.bitNum, that.bitNum)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        if (bitNum == 0) {
            return "";
        }
        StringBuilder bitVectorString = new StringBuilder(bigInteger.toString(2));
        while (bitVectorString.length() < bitNum) {
            bitVectorString.insert(0, "0");
        }
        return bitVectorString.toString();
    }
}

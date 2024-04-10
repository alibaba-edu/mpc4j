package edu.alibaba.mpc4j.common.tool.bitvector;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * The bit vector represented by bytes.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public class BytesBitVector implements BitVector {
    /**
     * bit vector represented by bytes.
     */
    private byte[] bytes;
    /**
     * number of bit.
     */
    private int bitNum;
    /**
     * number of byte.
     */
    private int byteNum;
    /**
     * the offset
     */
    private int offset;

    static BitVector create(int bitNum, byte[] bytes) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        assert bytes.length == byteLength : "bytes.length must be equal to " + byteLength + ": " + bytes.length;
        assert BytesUtils.isReduceByteArray(bytes, bitNum) : "bytes must contain at most " + bitNum + " bits";
        // create instance
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = bytes;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        bitVector.offset = bitVector.byteNum * Byte.SIZE - bitVector.bitNum;
        return bitVector;
    }

    static BitVector create(int bitNum, BigInteger bigInteger) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        assert BigIntegerUtils.greaterOrEqual(bigInteger, BigInteger.ZERO)
            : "bigInteger must be greater than or equal to 0: " + bigInteger;
        assert bigInteger.bitLength() <= bitNum
            : "bigInteger.bitLength must be less than or equal to " + bitNum + ": " + bigInteger.bitLength();
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create instance
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = BigIntegerUtils.nonNegBigIntegerToByteArray(bigInteger, byteLength);
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        bitVector.offset = bitVector.byteNum * Byte.SIZE - bitVector.bitNum;
        return bitVector;
    }

    static BitVector createRandom(int bitNum, Random random) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create random bytes
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        BytesUtils.reduceByteArray(bytes, bitNum);
        // create instance
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = bytes;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        bitVector.offset = bitVector.byteNum * Byte.SIZE - bitVector.bitNum;
        return bitVector;
    }

    static BitVector createOnes(int bitNum) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create bytes with all 1
        byte[] ones = new byte[byteLength];
        Arrays.fill(ones, (byte) 0xFF);
        BytesUtils.reduceByteArray(ones, bitNum);
        // create instance
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = ones;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        bitVector.offset = bitVector.byteNum * Byte.SIZE - bitVector.bitNum;
        return bitVector;
    }

    static BitVector createZeros(int bitNum) {
        assert bitNum > 0 : "the number of bits must be greater than 0: " + bitNum;
        int byteLength = CommonUtils.getByteLength(bitNum);
        // create bytes with all 0
        byte[] zeros = new byte[byteLength];
        // create instance
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = zeros;
        bitVector.bitNum = bitNum;
        bitVector.byteNum = byteLength;
        bitVector.offset = bitVector.byteNum * Byte.SIZE - bitVector.bitNum;
        return bitVector;
    }

    static BitVector createEmpty() {
        BytesBitVector bitVector = new BytesBitVector();
        bitVector.bytes = new byte[0];
        bitVector.bitNum = 0;
        bitVector.byteNum = 0;
        bitVector.offset = 0;
        return bitVector;
    }

    @Override
    public BitVectorType getType() {
        return BitVectorType.BYTES_BIT_VECTOR;
    }

    @Override
    public void set(int index, boolean value) {
        assert index >= 0 && index < bitNum : "index must be in range [0, " + bitNum + ")";
        BinaryUtils.setBoolean(bytes, index + offset, value);
    }

    @Override
    public boolean get(int index) {
        assert index >= 0 && index < bitNum : "index must be in range [0, " + bitNum + ")";
        return BinaryUtils.getBoolean(bytes, index + offset);
    }

    @Override
    public BitVector copy() {
        BytesBitVector copyBitVector = new BytesBitVector();
        copyBitVector.bytes = BytesUtils.clone(bytes);
        copyBitVector.bitNum = bitNum;
        copyBitVector.byteNum = byteNum;
        copyBitVector.offset = offset;

        return copyBitVector;
    }

    @Override
    public void replaceCopy(BitVector that) {
        assertEqualBitNum(that);
        byte[] thatBytes = that.getBytes();
        System.arraycopy(thatBytes, 0, bytes, 0, byteNum);
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
        return bytes;
    }

    @Override
    public BigInteger getBigInteger() {
        if (bitNum == 0) {
            return BigInteger.ZERO;
        } else {
            return BigIntegerUtils.byteArrayToNonNegBigInteger(bytes);
        }
    }

    @Override
    public BitVector split(int bitNum) {
        assert bitNum > 0 && bitNum <= this.bitNum
            : "number of split bits must be in range (0, " + this.bitNum + "]: " + bitNum;
        // 切分方法：分别对2^length取模数和取余数，模数作为split结果，余数作为剩余结果
        BigInteger mask = BigInteger.ONE.shiftLeft(this.bitNum - bitNum).subtract(BigInteger.ONE);
        // 由于模数一定是2^length格式，因此可以用位运算更高效地实现
        BigInteger remainBigInteger = getBigInteger();
        BigInteger splitBigInteger = remainBigInteger.shiftRight(this.bitNum - bitNum);
        remainBigInteger = remainBigInteger.and(mask);
        // update the remained bit vector
        this.bitNum = this.bitNum - bitNum;
        byteNum = this.bitNum == 0 ? 0 : CommonUtils.getByteLength(this.bitNum);
        bytes = BigIntegerUtils.nonNegBigIntegerToByteArray(remainBigInteger, byteNum);
        offset = byteNum * Byte.SIZE - this.bitNum;
        // return a new instance
        return BytesBitVector.create(bitNum, splitBigInteger);
    }

    @Override
    public void reduce(int bitNum) {
        assert bitNum > 0 && bitNum <= this.bitNum
            : "number of reduced bits must be in range (0, " + this.bitNum + "]: " + bitNum;
        if (bitNum < this.bitNum) {
            // compute number of reduced bytes, and set the remaining first byte as leading zeros.
            int remainByteNum = CommonUtils.getByteLength(bitNum);
            if (remainByteNum < byteNum) {
                bytes = BytesUtils.createReduceByteArray(bytes, bitNum);
                byteNum = remainByteNum;
            } else {
                bytes[0] &= (byte) ((1 << (bitNum & 7)) - 1);
            }
            // update other parameters
            offset = (remainByteNum << 3) - bitNum;
            this.bitNum = bitNum;
        }
    }

    @Override
    public void merge(BitVector that) {
        bitNum += that.bitNum();
        if (that.bitNum() == that.byteNum() * Byte.SIZE) {
            // if that BitVector can be represented as whole bytes, then directly merge bytes.
            byte[] resBytes = new byte[this.byteNum + that.byteNum()];
            System.arraycopy(this.getBytes(), 0, resBytes, 0, this.byteNum);
            System.arraycopy(that.getBytes(), 0, resBytes, this.byteNum, that.byteNum());
            bytes = resBytes;
            byteNum += that.byteNum();
        } else {
            BigInteger mergeBigInteger = that.getBigInteger();
            BigInteger remainBigInteger = getBigInteger();
            // shift the remained bit vector
            remainBigInteger = remainBigInteger.shiftLeft(that.bitNum()).or(mergeBigInteger);
            // update the remained bit vector
            byteNum = bitNum == 0 ? 0 : CommonUtils.getByteLength(bitNum);
            bytes = BigIntegerUtils.nonNegBigIntegerToByteArray(remainBigInteger, byteNum);
            offset = byteNum * Byte.SIZE - bitNum;
        }
    }

    @Override
    public BitVector xor(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BytesBitVector.createEmpty();
        } else {
            return BytesBitVector.create(bitNum, BytesUtils.xor(bytes, that.getBytes()));
        }
    }

    @Override
    public void xori(BitVector that) {
        assertEqualBitNum(that);
        BytesUtils.xori(bytes, that.getBytes());
    }

    @Override
    public BitVector and(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BytesBitVector.createEmpty();
        } else {
            return BytesBitVector.create(bitNum, BytesUtils.and(bytes, that.getBytes()));
        }
    }

    @Override
    public void andi(BitVector that) {
        assertEqualBitNum(that);
        BytesUtils.andi(bytes, that.getBytes());
    }

    @Override
    public BitVector or(BitVector that) {
        assertEqualBitNum(that);
        if (bitNum == 0) {
            return BytesBitVector.createEmpty();
        } else {
            return BytesBitVector.create(bitNum, BytesUtils.or(bytes, that.getBytes()));
        }
    }

    @Override
    public void ori(BitVector that) {
        assertEqualBitNum(that);
        BytesUtils.ori(bytes, that.getBytes());
    }

    @Override
    public BitVector not() {
        if (bitNum == 0) {
            return BytesBitVector.createEmpty();
        } else {
            return BytesBitVector.create(bitNum, BytesUtils.not(bytes, bitNum));
        }
    }

    @Override
    public void noti() {
        BytesUtils.noti(bytes, bitNum);
    }

    private void assertEqualBitNum(BitVector that) {
        assert bitNum == that.bitNum() : "the given bit vector must contain " + bitNum + " bits: " + that.bitNum();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getBytes())
            .append(bitNum())
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BitVector) {
            BitVector that = (BitVector) obj;
            return new EqualsBuilder()
                .append(this.getBytes(), that.getBytes())
                .append(this.bitNum(), that.bitNum())
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        if (bitNum == 0) {
            return "";
        }
        StringBuilder bitVectorString = new StringBuilder(getBigInteger().toString(2));
        while (bitVectorString.length() < bitNum) {
            bitVectorString.insert(0, "0");
        }
        return bitVectorString.toString();
    }

    @Override
    public void extendBitNum(int extendBitNum) {
        // we must ensure extendBitNum >= bitNum
        MathPreconditions.checkGreaterOrEqual("extendBitNum", extendBitNum, bitNum);
        int targetByteLength = CommonUtils.getByteLength(extendBitNum);
        if (byteNum < targetByteLength) {
            // in this case, we need to add more bytes.
            byte[] res = new byte[targetByteLength];
            System.arraycopy(bytes, 0, res, targetByteLength - byteNum, byteNum);
            bytes = res;
            byteNum = targetByteLength;
        }
        bitNum = extendBitNum;
        offset = (targetByteLength << 3) - extendBitNum;
    }

    @Override
    public BitVector padShiftLeft(int n) {
        MathPreconditions.checkNonNegative("n", n);
        int byteLen = CommonUtils.getByteLength(n + bitNum);
        byte[] newByte = new byte[byteLen];
        System.arraycopy(bytes, 0, newByte, byteLen - bytes.length, bytes.length);
        BytesUtils.shiftLefti(newByte, n);
        return create(bitNum + n, newByte);
    }

    @Override
    public void fixShiftLefti(int n) {
        MathPreconditions.checkNonNegative("n", n);
        BytesUtils.shiftLefti(bytes, n);
        byte andNum = (byte) ((bitNum & 7) == 0 ? 255 : (1 << (bitNum & 7)) - 1);
        bytes[0] &= andNum;
    }

    @Override
    public BitVector reduceShiftRight(int n) {
        MathPreconditions.checkNonNegativeInRangeClosed("n", n, bitNum);
        if (bitNum == n) {
            return createEmpty();
        } else {
            byte[] res = BytesUtils.copyByteArray(BytesUtils.shiftRight(bytes, n), CommonUtils.getByteLength(bitNum - n));
            return create(bitNum - n, res);
        }
    }

    @Override
    public void reduceShiftRighti(int n) {
        MathPreconditions.checkNonNegativeInRangeClosed("n", n, bitNum);
        if (bitNum == n) {
            bytes = new byte[0];
            bitNum = 0;
        } else {
            bitNum -= n;
            BytesUtils.shiftRighti(bytes, n);
            bytes = BytesUtils.copyByteArray(bytes, CommonUtils.getByteLength(bitNum));
        }
    }

    @Override
    public void fixShiftRighti(int n) {
        MathPreconditions.checkNonNegativeInRangeClosed("n", n, bitNum);
        BytesUtils.shiftRighti(bytes, n);
    }

    @Override
    public void setBytes(byte[] source, int srcPos, int thisPos, int byteLength) {
        MathPreconditions.checkNonNegative("srcPos", srcPos);
        MathPreconditions.checkNonNegative("byteLength", byteLength);
        MathPreconditions.checkLessOrEqual("srcPos + byteLength", srcPos + byteLength, source.length);
        MathPreconditions.checkLessOrEqual("thisPos + byteLength", thisPos + byteLength, bytes.length);
        System.arraycopy(source, srcPos, bytes, thisPos, byteLength);
    }

    @Override
    public BitVector[] uncheckSplitWithPadding(int[] bitNums) {
        BitVector[] res = new BitVector[bitNums.length];
        int k = 0;
        for (int i = 0; i < bitNums.length; i++) {
            int byteNum = CommonUtils.getByteLength(bitNums[i]);
            byte[] tmp = Arrays.copyOfRange(bytes, k, k + byteNum);
            // we directly reduce tmp, since operations may occur in the merged form so that the padding may not be zero.
            BytesUtils.reduceByteArray(tmp, bitNums[i]);
            res[i] = create(bitNums[i], tmp);
            k += byteNum;
        }
        // check that the bit vector is indeed merged by bitNums.
        MathPreconditions.checkEqual("k", "byteLength", k, bytes.length);

        return res;
    }

    @Override
    public void reverseBits() {
        byte[] reverseBytes = BytesUtils.reverseBitArray(bytes);
        int shiftNum = (bitNum & 7) > 0 ? 8 - (bitNum & 7) : 0;
        if (shiftNum > 0) {
            BytesUtils.shiftRighti(reverseBytes, shiftNum);
        }
        bytes = reverseBytes;
    }

    @Override
    public boolean numOf1IsOdd() {
        return (BytesUtils.bitCount(bytes) & 1) == 1;
    }
}

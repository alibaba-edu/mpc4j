package edu.alibaba.mpc4j.common.tool.utils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 字节{@code byte}和字节数组{@code byte[]}工具类。
 * <p>基础操作源代码来自Bouncy Castle的ByteUtils.java。移位操作源代码参考文章《循环移位：字节数组（byte[]）实现二进制串的循环移位》
 * （<a href="https://blog.csdn.net/weixin_40411846/article/details/79580431">...</a>）。
 *
 * @author Weiran Liu
 * @date 2021/06/19
 */
public class BytesUtils {
    static final byte[] BYTE_WITH_FIX_NUM_OF_ONE = new byte[]{
        0, 1, 3, 7, 15, 31, 63, 127
    };

    /**
     * 私有构造函数。
     */
    private BytesUtils() {
        // empty
    }

    private static byte innerReverseBit(final byte byteValue) {
        byte copyByteValue = byteValue;
        int reverseByteValue = 0;
        for (int i = 0; i < Byte.SIZE; i++, copyByteValue >>= 1) {
            reverseByteValue = reverseByteValue << 1 | (copyByteValue & 1);
        }
        return (byte) reverseByteValue;
    }

    /**
     * reverse bit array map
     */
    private static final byte[] REVERSE_BIT_ARRAY = new byte[1 << Byte.SIZE];

    static {
        for (int i = 0; i < (1 << Byte.SIZE); i++) {
            REVERSE_BIT_ARRAY[i] = innerReverseBit((byte) i);
        }
    }

    /**
     * reverse bits in the given byte, e.g., if byte value is 0b00000001, then the reversed byte value is 0b10000000.
     *
     * @param byteValue the given byte.
     * @return the reversed byte.
     */
    public static byte reverseBit(final byte byteValue) {
        return REVERSE_BIT_ARRAY[(int) byteValue & 0xFF];
    }

    /**
     * 调换字节数组的大小端表示。只调换字节数组，不调换字节数组中的元素。
     *
     * @param byteArray 给定的字节数组。
     * @return 调换结果。
     */
    public static byte[] reverseByteArray(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        byte[] reverseByteArray = BytesUtils.clone(byteArray);
        innerReverseByteArray(reverseByteArray);
        return reverseByteArray;
    }

    /**
     * 调换字节数组的大小端表示。只调换字节数组，不调换字节数组中的元素。
     *
     * @param byteArray 给定的字节数组。
     */
    public static void innerReverseByteArray(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }
        // 只调换位置
        int i = 0;
        int j = byteArray.length - 1;
        byte tmp;
        while (j > i) {
            tmp = byteArray[j];
            byteArray[j] = byteArray[i];
            byteArray[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * 调换字节数组的大小端表示。既调换字节数组，又调换字节数组中的每一个比特。
     *
     * @param byteArray 给定的字节数组。
     * @return 调换结果。
     */
    public static byte[] reverseBitArray(final byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        byte[] result = new byte[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            result[byteArray.length - 1 - i] = reverseBit(byteArray[i]);
        }
        return result;
    }

    /**
     * 内部调换字节数组的大小端表示。既调换字节数组，又调换字节数组中的每一个比特。
     *
     * @param byteArray 给定的字节数组。
     */
    public static void innerReverseBitArray(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }
        // 先调换位置
        innerReverseByteArray(byteArray);
        // 再每个字节分别调换
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = reverseBit(byteArray[i]);
        }
    }

    /**
     * 字节中包含1个数的查找表，即BYTE_BIT_COUNT_TABLE[byteValue]表示byteValue包含1的个数。
     */
    private static final int[] BYTE_BIT_COUNT_TABLE = IntStream.range(0, 1 << Byte.SIZE)
        .map(byteValue -> {
            boolean[] binaryValue = BinaryUtils.byteToBinary((byte) (byteValue & 0xFF));
            int bitCount = 0;
            for (boolean b : binaryValue) {
                bitCount = b ? bitCount + 1 : bitCount;
            }
            return bitCount;
        }).toArray();

    /**
     * 得到给定{@code byte[]}中1的个数。
     *
     * @param byteArray 给定的{@code byte[]}。
     * @return 给定{@code byte[]}中1的个数。
     */
    public static int bitCount(final byte[] byteArray) {
        int count = 0;
        for (byte byteValue : byteArray) {
            count += BYTE_BIT_COUNT_TABLE[(byteValue & 0xFF)];
        }
        return count;
    }

    /**
     * 将给定的{@code byte[]}修正为有效位数是{@code bitLength}的{@code byte[]}，大端表示。
     *
     * @param byteArray 给定的{@code byte[]}。
     * @param bitLength 有效比特位数。
     */
    public static void reduceByteArray(byte[] byteArray, final int bitLength) {
        // 这里的bitLength指的是要保留多少个比特位，因此可以取到[0, byteArray.length * Byte.SIZE]
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        int resBitNum = bitLength & 7;
        int zeroByteNum = (byteArray.length * Byte.SIZE - bitLength) >> 3;
        Arrays.fill(byteArray, 0, zeroByteNum, (byte) 0x00);
        if (resBitNum != 0) {
            byteArray[zeroByteNum] &= BYTE_WITH_FIX_NUM_OF_ONE[resBitNum];
        }
    }

    /**
     * Creates a new byte array that is reduced from the given byte array with {@code bitLength} valid bits. The length
     * of the returned byte array is automatically truncated to fit {@code bitLength} if necessary.
     *
     * @param byteArray given {@code byte[]}.
     * @param bitLength number of valid bits.
     * @return reduced byte array.
     */
    public static byte[] createReduceByteArray(byte[] byteArray, final int bitLength) {
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        int resBitNum = bitLength & 7;
        int byteNum = CommonUtils.getByteLength(bitLength);
        byte[] res = Arrays.copyOfRange(byteArray, byteArray.length - byteNum, byteArray.length);
        if (resBitNum != 0) {
            res[0] &= BYTE_WITH_FIX_NUM_OF_ONE[resBitNum];
        }
        return res;
    }

    /**
     * Verify that the given {@code byte[]} contains at most {@code bitLength} valid bits.
     * The bits are represented in Big-endian format.
     * <p>
     * Here we allow the length of the given {@code byte[]} can be greater than the byte length of {@code bitLength}.
     * For example,
     * <li>it returns true when {@code byteArray = {0x01}} and {@code bitLength = 1}.</li>
     * <li>it returns true when {@code byteArray = {0x00, 0x01}} and {@code bitLength = 1}.</li>
     * <li>it returns false when {@code byteArray = {0x00, 0x02}} and {@code bitLength = 1}.</li>
     * <li>it throws a Runtime Exception when {@code byteArray = {0x01}} and {@code bitLength = 9}.</li>
     * </p>
     *
     * @param byteArray the given {@code byte[]}.
     * @param bitLength the expected bit length.
     * @return true if the given {@code byte[]} contains at most {@code bitLength} valid bits.
     */
    public static boolean isReduceByteArray(byte[] byteArray, final int bitLength) {
        // 这里的bitLength指的是要保留多少个比特位，因此可以取到[0, byteArray.length * Byte.SIZE]
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        int resBitNum = bitLength & 7;
        int zeroByteNum = (byteArray.length * Byte.SIZE - bitLength) >> 3;
        for (int byteIndex = 0; byteIndex < zeroByteNum; byteIndex++) {
            if (byteArray[byteIndex] != 0) {
                return false;
            }
        }
        // 如果没有前面几位需要置为0的byte，或者前面若干位确实是0，则返回true
        return resBitNum == 0 || (byteArray[zeroByteNum] & BYTE_WITH_FIX_NUM_OF_ONE[resBitNum]) == byteArray[zeroByteNum];
    }

    /**
     * Verify that the given {@code byte[]} has the fixed size and contains at most {@code bitLength} valid bits.
     * The bits are represented in Big-endian format.
     *
     * @param byteArray  the given {@code byte[]}.
     * @param byteLength the expected byte length.
     * @param bitLength  the expected bit length.
     * @return true the given {@code byte[]} has the fixed size and contains at most {@code bitLength} valid bits.
     */
    public static boolean isFixedReduceByteArray(byte[] byteArray, final int byteLength, final int bitLength) {
        assert byteLength >= 0 : "byteLength must be greater than or equal to 0: " + byteLength;
        if (byteArray.length != byteLength) {
            return false;
        }
        return isReduceByteArray(byteArray, bitLength);
    }

    /**
     * Creates an all-one byte array.
     *
     * @param bitLength bit length.
     * @return an all-one byte array.
     */
    public static byte[] allOneByteArray(int bitLength) {
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[] vector = new byte[byteLength];
        Arrays.fill(vector, (byte) 0xFF);
        if ((bitLength & 7) != 0) {
            vector[0] = BYTE_WITH_FIX_NUM_OF_ONE[bitLength & 7];
        }
        return vector;
    }

    /**
     * Generates a random byte array.
     *
     * @param byteLength   the byte length.
     * @param bitLength    the bit length.
     * @param secureRandom the random state.
     * @return a random byte array.
     */
    public static byte[] randomByteArray(final int byteLength, final int bitLength, SecureRandom secureRandom) {
        assert byteLength * Byte.SIZE >= bitLength
            : "bitLength = " + bitLength + ", byteLength does not have enough room: " + byteLength;
        byte[] byteArray = new byte[byteLength];
        secureRandom.nextBytes(byteArray);
        reduceByteArray(byteArray, bitLength);
        return byteArray;
    }

    /**
     * Generates a random byte array.
     *
     * @param byteLength   byte length.
     * @param secureRandom random state.
     * @return a random byte array.
     */
    public static byte[] randomByteArray(final int byteLength, SecureRandom secureRandom) {
        assert byteLength > 0 : "byteLength must be greater than 0: " + byteLength;
        byte[] byteArray = new byte[byteLength];
        secureRandom.nextBytes(byteArray);
        return byteArray;
    }

    /**
     * Creates random byte array vector.
     *
     * @param length       vector length.
     * @param byteLength   byte length.
     * @param secureRandom random state.
     * @return a random byte array vector.
     */
    public static byte[][] randomByteArrayVector(final int length, final int byteLength, SecureRandom secureRandom) {
        return IntStream.range(0, length)
            .mapToObj(index -> BytesUtils.randomByteArray(byteLength, secureRandom))
            .toArray(byte[][]::new);
    }

    /**
     * 在给定{@code byte[]}前填充0x00到指定的长度。如果指定长度等于给定{@code byte[]}的长度，则直接返回结果，否则将进行复制。
     *
     * @param byteArray 给定的{@code byte[]}。
     * @param length    目标长度。
     * @return 填充到指定长度的{@code byte[]}。
     */
    public static byte[] paddingByteArray(byte[] byteArray, int length) {
        assert byteArray.length <= length : "length of byte[] must be less than " + length + ": " + byteArray.length;
        if (byteArray.length == length) {
            return byteArray;
        }
        byte[] paddingByteArray = new byte[length];
        System.arraycopy(byteArray, 0, paddingByteArray, length - byteArray.length, byteArray.length);
        return paddingByteArray;
    }

    /**
     * Copies the specified array, truncating (in front) or padding (in front) with zeros (if necessary) so the copy
     * has the specified length.
     *
     * @param byteArray the array to be copied.
     * @param length    the length of the copy to be returned.
     * @return a copy of the original array, truncated (in the front) or padded with zeros (in the front) to obtain the
     * specified length.
     */
    public static byte[] copyByteArray(byte[] byteArray, int length) {
        assert length > 0 : "length of byte[] must be greater than 0: " + length;
        if (byteArray.length < length) {
            return paddingByteArray(byteArray, length);
        } else {
            return Arrays.copyOfRange(byteArray, byteArray.length - length, byteArray.length);
        }
    }

    /**
     * 比较两个字节数组。
     *
     * @param left  第一个字节数组。
     * @param right 第二个字节数组。
     * @return 比较结果。
     */
    public static boolean equals(final byte[] left, final byte[] right) {
        if (left == null) {
            return right == null;
        }
        if (right == null) {
            return false;
        }
        if (left.length != right.length) {
            return false;
        }
        // 从最低位开始比较，只要有一个byte不相等就立刻返回
        for (int i = left.length - 1; i >= 0; i--) {
            if (left[i] != right[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回给定{@code byte[]}的克隆结果。
     *
     * @param byteArray 待克隆的{@code byte[]}。
     * @return {@code byte[]}的克隆结果。如果待克隆的{@code byte[]}为null，则返回null。
     */
    public static byte[] clone(final byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    /**
     * 从偏移量开始拷贝指定长度的字节数组。
     *
     * @param buf 字节数组。
     * @param off 字节数组偏移量。
     * @param len 拷贝长度。
     * @return 拷贝结果。
     */
    public static byte[] clone(final byte[] buf, int off, int len) {
        byte[] result = new byte[len];
        System.arraycopy(buf, off, result, 0, len);
        return result;
    }

    /**
     * 返回给定{@code byte[][]}的克隆结果。
     *
     * @param byteArrays 待克隆的{@code byte[][]}。
     * @return {@code byte[][]}的克隆结果。如果待克隆的{@code byte[][]}为null，则返回null。
     */
    public static byte[][] clone(final byte[][] byteArrays) {
        if (byteArrays == null) {
            return null;
        }
        return Arrays.stream(byteArrays).map(BytesUtils::clone).toArray(byte[][]::new);
    }

    /**
     * 返回给定{@code byte[][][]}的克隆结果。
     *
     * @param byteArrays 待克隆的{@code byte[][][]}。
     * @return {@code byte[][][]}的克隆结果。如果待克隆的{@code byte[][][]}为null，则返回null。
     */
    public static byte[][][] clone(final byte[][][] byteArrays) {
        if (byteArrays == null) {
            return null;
        }
        return Arrays.stream(byteArrays).map(BytesUtils::clone).toArray(byte[][][]::new);
    }

    /**
     * 计算两个字节数组的XOR结果。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     * @return x1 XOR x2。
     */
    public static byte[] xor(final byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte) (x1[i] ^ x2[i]);
        }

        return out;
    }

    /**
     * 计算两个字节数组的XOR结果，并把结果放在第一个字节数组上。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     */
    public static void xori(byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (byte) (x1[i] ^ x2[i]);
        }
    }

    /**
     * 计算两个字节数组的AND结果。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     * @return x1 AND x2。
     */
    public static byte[] and(final byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte) (x1[i] & x2[i]);
        }

        return out;
    }

    /**
     * 计算两个字节数组的AND结果，并把结果放在第一个字节数组上。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     */
    public static void andi(byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (byte) (x1[i] & x2[i]);
        }
    }

    /**
     * 计算两个字节数组的OR结果。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     * @return x1 OR x2。
     */
    public static byte[] or(final byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte) (x1[i] | x2[i]);
        }

        return out;
    }

    /**
     * 计算两个字节数组的OR结果，并把结果放在第一个字节数组上。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     */
    public static void ori(byte[] x1, final byte[] x2) {
        assert x1.length == x2.length : "x1.length = " + x1.length + " must be equal to x2.length = " + x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (byte) (x1[i] | x2[i]);
        }
    }

    /**
     * 计算节数组的NOT结果。
     *
     * @param x         字节数组。
     * @param bitLength 比特长度。
     * @return NOT x。
     */
    public static byte[] not(final byte[] x, final int bitLength) {
        assert bitLength >= 0 && bitLength <= x.length * Byte.SIZE;
        byte[] ones = new byte[x.length];
        int resBitNum = bitLength & 7;
        int zeroByteNum = (x.length * Byte.SIZE - bitLength) >> 3;
        Arrays.fill(ones, zeroByteNum, ones.length, (byte) 0xff);
        if (resBitNum != 0) {
            ones[zeroByteNum] = BYTE_WITH_FIX_NUM_OF_ONE[resBitNum];
        }
        return BytesUtils.xor(x, ones);
    }


    /**
     * 计算节数组的NOT结果，并把结果更新在字节数组上。要求{@code bitLength <= x.length * Byte.SIZE}但不会验证。
     *
     * @param x         字节数组。
     * @param bitLength 比特长度。
     */
    public static void noti(byte[] x, final int bitLength) {
        byte[] ones = new byte[x.length];
        int resBitNum = bitLength & 7;
        int zeroByteNum = (x.length * Byte.SIZE - bitLength) >> 3;
        Arrays.fill(ones, zeroByteNum, ones.length, (byte) 0xff);
        if (resBitNum != 0) {
            ones[zeroByteNum] = BYTE_WITH_FIX_NUM_OF_ONE[resBitNum];
        }
        xori(x, ones);
    }

    /**
     * shift right.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     * @return result.
     */
    public static byte[] shiftRight(final byte[] byteArray, final int x) {
        assert x >= 0;
        if (x == 0) {
            // x = 0, byte array is unchanged.
            return clone(byteArray);
        }
        if (x >= byteArray.length * Byte.SIZE) {
            // x is so large that result must be all 0
            return new byte[byteArray.length];
        }
        int binaryMove = x % Byte.SIZE;
        int byteMove = (x - binaryMove) / Byte.SIZE;
        byte[] shiftRightByteArray = new byte[byteArray.length];
        // shift by bytes
        System.arraycopy(byteArray, 0, shiftRightByteArray, byteMove, byteArray.length - byteMove);
        // shift by bits
        if (binaryMove != 0) {
            binaryShiftRight(shiftRightByteArray, binaryMove);
        }
        return shiftRightByteArray;
    }

    /**
     * in-place shift right.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     */
    public static void shiftRighti(byte[] byteArray, final int x) {
        assert x >= 0;
        if (x == 0) {
            // x = 0, byte array is unchanged.
            return;
        }
        if (x >= byteArray.length * Byte.SIZE) {
            // x is so large that result must be all 0
            Arrays.fill(byteArray, (byte) 0x00);
        }
        int binaryMove = x % Byte.SIZE;
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // shift by bytes, note that we must clean higher bytes
        System.arraycopy(byteArray, 0, byteArray, byteMove, byteArray.length - byteMove);
        for (int i = 0; i < byteMove; i++) {
            byteArray[i] = 0x00;
        }
        // shift by bits
        if (binaryMove != 0) {
            binaryShiftRight(byteArray, binaryMove);
        }
    }

    private static void binaryShiftRight(byte[] byteArray, final int x) {
        assert x >= 0 && x < Byte.SIZE : "x must be in range [0, " + Byte.SIZE + ")";
        for (int i = byteArray.length - 1, supplyShiftBit = Byte.SIZE - x; i > 0; i--) {
            // shift current byte
            int currentByte = (byteArray[i] & 0xFF) >>> x;
            // supply from next byte
            int supplyByte = (byteArray[i - 1] & 0xFF) << supplyShiftBit;
            byteArray[i] = (byte) (currentByte | supplyByte);
        }
        // handle the last byte
        byteArray[0] = (byte) ((byteArray[0] & 0xFF) >>> x);
    }

    /**
     * shift left.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     * @return result.
     */
    public static byte[] shiftLeft(final byte[] byteArray, final int x) {
        assert x >= 0;
        if (x == 0) {
            // x = 0, byte array is unchanged.
            return clone(byteArray);
        }
        if (x >= byteArray.length * Byte.SIZE) {
            // x is so large that result must be all 0
            return new byte[byteArray.length];
        }
        int binaryMove = x % Byte.SIZE;
        int byteMove = (x - binaryMove) / Byte.SIZE;
        byte[] resultByteArray = new byte[byteArray.length];
        // shift by bytes
        System.arraycopy(byteArray, byteMove, resultByteArray, 0, byteArray.length - byteMove);
        // shift by bits
        if (binaryMove != 0) {
            binaryShiftLeft(resultByteArray, binaryMove);
        }
        return resultByteArray;
    }

    /**
     * in-place shift left.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     */
    public static void shiftLefti(byte[] byteArray, final int x) {
        assert x >= 0;
        if (x == 0) {
            // x = 0, byte array is unchanged.
            return;
        }
        if (x >= byteArray.length * Byte.SIZE) {
            // x is so large that result must be all 0
            Arrays.fill(byteArray, (byte) 0x00);
        }
        int binaryMove = x % Byte.SIZE;
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // shift by bytes, note that we must clean lower bytes
        System.arraycopy(byteArray, byteMove, byteArray, 0, byteArray.length - byteMove);
        for (int i = byteArray.length - byteMove; i < byteArray.length; i++) {
            byteArray[i] = 0x00;
        }
        // shift by bits
        if (binaryMove != 0) {
            binaryShiftLeft(byteArray, binaryMove);
        }
    }

    private static void binaryShiftLeft(byte[] byteArray, final int x) {
        assert x >= 0 && x < Byte.SIZE : "x must be in range [0, " + Byte.SIZE + ")";
        for (int i = 0, supplyShiftBit = Byte.SIZE - x; i < byteArray.length - 1; i++) {
            // shift current byte
            int currentByte = (byteArray[i] & 0xFF) << x;
            // supply from next byte
            int supplyByte = (byteArray[i + 1] & 0xFF) >> supplyShiftBit;
            // combine
            byteArray[i] = (byte) (currentByte | supplyByte);
        }
        // handle last byte
        byteArray[byteArray.length - 1] = (byte) ((byteArray[byteArray.length - 1] & 0xFF) << x);
    }

    /**
     * 求x和y的内积。
     *
     * @param x           用{@code byte[][]}表示的x。
     * @param xByteLength x中每个元素的字节长度。
     * @param y           用{@code boolean[]}表示的y。
     * @return x和y的内积。
     */
    public static byte[] innerProduct(byte[][] x, int xByteLength, boolean[] y) {
        assert x.length == y.length;
        byte[] value = new byte[xByteLength];
        for (int i = 0; i < x.length; i++) {
            if (y[i]) {
                xori(value, x[i]);
            }
        }
        return value;
    }

    /**
     * Computes the inner product of x and y (positions labeled as 1).
     *
     * @param x           vector x.
     * @param xByteLength x byte length.
     * @param positions   vector y (positions labeled as 1).
     * @return the inner product of x and y.
     */
    public static byte[] innerProduct(byte[][] x, int xByteLength, int[] positions) {
        byte[] value = new byte[xByteLength];
        for (int position : positions) {
            BytesUtils.xori(value, x[position]);
        }
        return value;
    }

    /**
     * Computes the inner product of x and y.
     *
     * @param x           vector x.
     * @param xByteLength x byte length.
     * @param y           vector y.
     * @return the inner product of x and y.
     */
    public static byte[] innerProduct(byte[][] x, int xByteLength, byte[] y) {
        int num = x.length;
        int byteNum = CommonUtils.getByteLength(num);
        int offsetNum = byteNum * Byte.SIZE - num;
        assert BytesUtils.isFixedReduceByteArray(y, byteNum, num);
        byte[] value = new byte[xByteLength];
        for (int i = 0; i < x.length; i++) {
            if (BinaryUtils.getBoolean(y, offsetNum + i)) {
                xori(value, x[i]);
            }
        }
        return value;
    }

    /**
     * 计算a和b的汉明距离。
     *
     * @param a 字节数组a。
     * @param b 字节数组b。
     * @return 汉明距离。
     */
    public static int hammingDistance(byte[] a, byte[] b) {
        assert a.length == b.length;
        boolean[] xor = BinaryUtils.byteArrayToBinary(BytesUtils.xor(a, b));
        int hammingDistance = 0;
        for (boolean bit : xor) {
            hammingDistance = bit ? hammingDistance + 1 : hammingDistance;
        }
        return hammingDistance;
    }
}

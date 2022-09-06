package edu.alibaba.mpc4j.common.tool.utils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 字节{@code byte}和字节数组{@code byte[]}工具类。
 * <p>基础操作源代码来自Bouncy Castle的ByteUtils.java。移位操作源代码参考文章《循环移位：字节数组（byte[]）实现二进制串的循环移位》
 * （https://blog.csdn.net/weixin_40411846/article/details/79580431）。
 *
 * @author Weiran Liu
 * @date 2021/06/19
 */
public class BytesUtils {
    /**
     * 私有构造函数。
     */
    private BytesUtils() {
        // empty
    }

    /**
     * 调换字节的大小端表示，即如果输入为0b00000001，则输出为0b10000000。
     *
     * @param byteValue 给定的{@code byte}。
     * @return 调换结果。
     */
    public static byte reverseBit(final byte byteValue) {
        byte copyByteValue = byteValue;
        int reverseByteValue = 0;
        for (int i = 0; i < Byte.SIZE; i++, copyByteValue >>= 1) {
            reverseByteValue = reverseByteValue << 1 | (copyByteValue & 1);
        }
        return (byte)reverseByteValue;
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
            boolean[] binaryValue = BinaryUtils.byteToBinary((byte)(byteValue & 0xFF));
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
        for (int binaryIndex = 0; binaryIndex < byteArray.length * Byte.SIZE - bitLength; binaryIndex++) {
            BinaryUtils.setBoolean(byteArray, binaryIndex, false);
        }
    }

    /**
     * 验证给定{@code byte[]}的有效位数是否为{@code bitLength}，大端表示。
     *
     * @param byteArray 给定的{@code byte[]}。
     * @param bitLength 期望的比特长度。
     */
    public static boolean isReduceByteArray(byte[] byteArray, final int bitLength) {
        // 这里的bitLength指的是要保留多少个比特位，因此可以取到[0, byteArray.length * Byte.SIZE]
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        for (int binaryIndex = 0; binaryIndex < byteArray.length * Byte.SIZE - bitLength; binaryIndex++) {
            if (BinaryUtils.getBoolean(byteArray, binaryIndex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成指定比特长度的字节数组。
     *
     * @param bitLength 比特长度。
     * @param byteLength 字节长度。
     * @param secureRandom 随机状态。
     * @return 指定比特长度的字节数组。
     */
    public static byte[] randomByteArray(final int bitLength, final int byteLength, SecureRandom secureRandom) {
        assert byteLength * Byte.SIZE >= bitLength
            : "bitLength = " + bitLength + ", byteLength does not have enough room: " + byteLength;
        byte[] byteArray = new byte[byteLength];
        secureRandom.nextBytes(byteArray);
        reduceByteArray(byteArray, bitLength);
        return byteArray;
    }

    /**
     * 在给定{@code byte[]}前填充0x00到指定的长度。如果指定长度等于给定{@code byte[]}的长度，则直接返回结果，否则将进行复制。
     *
     * @param byteArray 给定的{@code byte[]}。
     * @param length 目标长度。
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
     * 计算两个字节数组的XOR结果。
     *
     * @param x1 第一个字节数组。
     * @param x2 第二个字节数组。
     * @return x1 XOR x2。
     */
    public static byte[] xor(final byte[] x1, final byte[] x2) {
        assert x1.length == x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte)(x1[i] ^ x2[i]);
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
            x1[i] = (byte)(x1[i] ^ x2[i]);
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
        assert x1.length == x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte)(x1[i] & x2[i]);
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
        assert x1.length == x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (byte)(x1[i] & x2[i]);
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
        assert x1.length == x2.length;
        byte[] out = new byte[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (byte)(x1[i] | x2[i]);
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
        assert x1.length == x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (byte)(x1[i] | x2[i]);
        }
    }

    /**
     * 计算节数组的NOT结果。
     *
     * @param x 字节数组。
     * @param bitLength 比特长度。
     * @return NOT x。
     */
    public static byte[] not(final byte[] x, final int bitLength) {
        assert bitLength >= 0 && bitLength <= x.length * Byte.SIZE;
        byte[] ones = new byte[x.length];
        Arrays.fill(ones, (byte)0xff);
        reduceByteArray(ones, bitLength);

        return BytesUtils.xor(x, ones);
    }


    /**
     * 计算节数组的NOT结果，并把结果更新在字节数组上。要求{@code bitLength <= x.length * Byte.SIZE}但不会验证。
     *
     * @param x 字节数组。
     * @param bitLength 比特长度。
     */
    public static void noti(byte[] x, final int bitLength) {
        byte[] ones = new byte[x.length];
        Arrays.fill(ones, (byte)0xff);
        reduceByteArray(ones, bitLength);
        xori(x, ones);
    }

    /**
     * 利用{@code byte[]}实现右移。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特长度。
     * @return 右移结果。
     */
    public static byte[] shiftRight(final byte[] byteArray, final int x) {
        assert x >= 0;
        // 如果右移0位，直接返回原始结果
        if (x == 0) {
            return clone(byteArray);
        }
        // 如果右移的位数超过了字节数组的比特长度，则返回全0字节数组
        if (x >= byteArray.length * Byte.SIZE) {
            return new byte[byteArray.length];
        }
        // 移动的比特数
        int binaryMove = x % Byte.SIZE;
        // 移动的字节数
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // 构建结果数组
        byte[] shiftRightByteArray = new byte[byteArray.length];
        // 先把需要移动的字节比特移动到位
        System.arraycopy(byteArray, 0, shiftRightByteArray, byteMove, byteArray.length - byteMove);
        // 移动剩余的比特长度
        if (binaryMove != 0) {
            binaryShiftRight(shiftRightByteArray, binaryMove);
        }
        return shiftRightByteArray;
    }

    /**
     * 利用{@code byte[]}实现右移，并将右移结果放置在{@code byte[]}中。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特数。
     */
    public static void shiftRighti(byte[] byteArray, final int x) {
        assert x >= 0;
        // 如果右移0位，直接返回原始结果
        if (x == 0) {
            return;
        }
        // 如果右移的位数超过了字节数组的比特长度，则返回全0字节数组
        if (x >= byteArray.length * Byte.SIZE) {
            Arrays.fill(byteArray, (byte)0x00);
        }
        // 移动的比特数
        int binaryMove = x % Byte.SIZE;
        // 移动的字节数
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // 先把需要移动的字节比特移动到位
        System.arraycopy(byteArray, 0, byteArray, byteMove, byteArray.length - byteMove);
        // 移动剩余的比特长度
        if (binaryMove != 0) {
            binaryShiftRight(byteArray, binaryMove);
        }
    }

    /**
     * 二进制右移。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特数，要求0 <= {@code x} < {@code Byte.SIZE}。
     */
    private static void binaryShiftRight(byte[] byteArray, final int x) {
        for (int i = byteArray.length - 1; i > 0; i--) {
            // 当前位置右移后的字节
            int currentByte = (byteArray[i] & 0xFF) >>> x;
            // 上一个位置补充到当前位置的字节
            int suppleByte = (byteArray[i - 1] & 0xFF) << (Byte.SIZE - x);
            byteArray[i] = (byte) (currentByte | suppleByte);
        }
        // 处理最后一个字节
        byteArray[0]=(byte)((byteArray[0] & 0xFF) >>> x);
    }

    /**
     * 利用{@code byte[]}实现左移。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特数。
     * @return 左移结果。
     */
    public static byte[] shiftLeft(final byte[] byteArray, final int x) {
        assert x >= 0;
        // 如果左移0位，直接返回原始结果
        if (x == 0) {
            return clone(byteArray);
        }
        // 如果左移的位数超过了字节数组的比特长度，则返回全0字节数组
        if (x >= byteArray.length * Byte.SIZE) {
            return new byte[byteArray.length];
        }
        // 移动的比特数
        int binaryMove = x % Byte.SIZE;
        // 移动的字节数
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // 构建结果数组
        byte[] resultByteArray = new byte[byteArray.length];
        // 先把需要移动的字节比特移动到位
        System.arraycopy(byteArray, byteMove, resultByteArray, 0, byteArray.length - byteMove);
        // 移动剩余的比特长度
        if (binaryMove != 0) {
            binaryShiftLeft(resultByteArray, binaryMove);
        }
        return resultByteArray;
    }

    /**
     * 利用{@code byte[]}实现左移，并将左移结果放置在{@code byte[]}中。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特数。
     */
    public static void shiftLefti(byte[] byteArray, final int x) {
        assert x >= 0;
        // 如果左移0位，直接返回原始结果
        if (x == 0) {
            return;
        }
        // 如果左移的位数超过了字节数组的比特长度，则返回全0字节数组
        if (x >= byteArray.length * Byte.SIZE) {
            Arrays.fill(byteArray, (byte)0x00);
        }
        // 移动的比特数
        int binaryMove = x % Byte.SIZE;
        // 移动的字节数
        int byteMove = (x - binaryMove) / Byte.SIZE;
        // 先把需要移动的字节比特移动到位
        System.arraycopy(byteArray, byteMove, byteArray, 0, byteArray.length - byteMove);
        // 移动剩余的比特长度
        if (binaryMove != 0) {
            binaryShiftLeft(byteArray, binaryMove);
        }
    }

    /**
     * 二进制左移。
     *
     * @param byteArray 字节数组。
     * @param x 移动的比特数，要求0 <= {@code x} < {@code Byte.SIZE}。
     */
    private static void binaryShiftLeft(byte[] byteArray, final int x) {
        for (int i = 0; i < byteArray.length - 1; i++) {
            // 当前位置左移后的字节
            int currentByte = (byteArray[i] & 0xFF) << x;
            // 下一个位置补充到当前位置的字节
            int suppleByte = (byteArray[i + 1] & 0xFF) >> (Byte.SIZE - x);
            // 两个结果合并
            byteArray[i] = (byte) (currentByte | suppleByte);
        }
        // 处理最后一个字节
        byteArray[byteArray.length - 1] = (byte)((byteArray[byteArray.length - 1] & 0xFF) << x);
    }

    /**
     * 求x和y的内积。
     *
     * @param x 用{@code byte[][]}表示的x。
     * @param xByteLength x中每个元素的字节长度。
     * @param y 用{@code boolean[]}表示的y。
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

package edu.alibaba.mpc4j.common.tool.utils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

/**
 * 整数工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class IntUtils {
    /**
     * 私有构造函数
     */
    private IntUtils() {
        // empty
    }

    /**
     * 将{@code int}转换为{@code byte[]}，大端表示。
     *
     * @param value 给定的{@code int}。
     * @return 转换结果。
     */
    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    /**
     * 将{@code byte[]}转换为{@code int}，大端表示。
     *
     * @param value 给定的{@code byte[]}。
     * @return 转换结果。
     */
    public static int byteArrayToInt(byte[] value) {
        assert value.length == Integer.BYTES;
        return ByteBuffer.wrap(value).getInt();
    }

    /**
     * 将{@code int}转换为{@code byte[]}，尽可能使用较小的转换长度。
     *
     * @param value 给定的{@code int}。
     * @param bound value的最大取值（包括此最大值）。
     * @return 转换结果。
     */
    public static byte[] boundedIntToByteArray(int value, int bound) {
        assert (value >= 0 && value <= bound);
        if (bound <= Byte.MAX_VALUE) {
            return ByteBuffer.allocate(Byte.BYTES).put((byte) value).array();
        } else if (bound <= Short.MAX_VALUE) {
            return ByteBuffer.allocate(Short.BYTES).putShort((short) value).array();
        } else {
            return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
        }
    }

    /**
     * 将{@code byte[]}转换为{@code int}，尽可能使用较小的转换长度。
     *
     * @param bytes 给定的{@code byte[]}。
     * @param bound value的最大取值（包括此最大值）。
     * @return 转换结果。
     */
    public static int byteArrayToBoundedInt(byte[] bytes, int bound) {
        int output;
        if (bound <= Byte.MAX_VALUE) {
            assert bytes.length == Byte.BYTES;
            output = ByteBuffer.wrap(bytes).get();
        } else if (bound <= Short.MAX_VALUE) {
            assert bytes.length == Short.BYTES;
            output = ByteBuffer.wrap(bytes).getShort();
        } else {
            assert bytes.length == Integer.BYTES;
            output = ByteBuffer.wrap(bytes).getInt();
        }
        assert output >= 0;
        return output;
    }

    /**
     * 将{@code int}转换为指定长度的{@code byte[]}，大端表示，不能用于负数。
     * - 如果指定长度{@code byteLength}小于{@code Integer.BYTES}，则在前面截断。
     * - 如果指定长度{@code byteLength}大于{@code Integer.BYTES}，则在前面补0.
     *
     * @param value      给定的{@code int}。
     * @param byteLength 要求字节长度。
     * @return 转换结果。
     */
    public static byte[] nonNegIntToFixedByteArray(int value, int byteLength) {
        assert value >= 0;
        assert byteLength > 0;
        if (byteLength >= Integer.BYTES) {
            // 如果要求字节长度大于等于Integer.BYTES，直接分配更大的内存空间，转换后返回结果
            return ByteBuffer.allocate(byteLength).putInt(byteLength - Integer.BYTES, value).array();
        } else {
            // 用assert验证一下value转换后的截断结果不会越界
            assert value <= (1 << byteLength * Byte.SIZE) - 1;
            // 如果要求字节长度小于Integer.BYTES，则需要多复制一次
            byte[] byteArray = ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
            byte[] output = new byte[byteLength];
            System.arraycopy(byteArray, byteArray.length - output.length, output, 0, output.length);
            return output;
        }
    }

    /**
     * 将指定长度的{@code byte[]}转换为{@code int}，大端表示。转换结果一定为正整数。
     * - 如果{@code byte[]}的长度小于{@code Integer.BYTES}，则在前面补0后转换。
     * - 如果{@code byte[]}的长度大于{@code Integer.BYTES}，则支取最后{@code Integer.BYTES}个字节转换。
     *
     * @param value 给定的{@code byte[]}。
     * @return 转换结果。
     */
    public static int fixedByteArrayToNonNegInt(byte[] value) {
        assert value.length > 0;
        if (value.length >= Integer.BYTES) {
            // 如果超过了表示范围，只取最后4位
            int output = ByteBuffer.wrap(value).getInt(value.length - Integer.BYTES);
            assert output >= 0;
            return output;
        } else {
            // 如果不够表示范围，则扩展到4位
            byte[] paddingValue = new byte[Integer.BYTES];
            System.arraycopy(value, 0, paddingValue, paddingValue.length - value.length, value.length);
            int output = ByteBuffer.wrap(paddingValue).getInt();
            assert output >= 0;
            return output;
        }
    }

    /**
     * 将{@code int[]}转换为{@code byte[]}。
     *
     * @param intArray 待转换的{@code int[]}。
     * @return 转换结果。
     */
    public static byte[] intArrayToByteArray(int[] intArray) {
        assert intArray.length > 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArray.length * Integer.BYTES);
        IntStream.range(0, intArray.length).forEach(index -> byteBuffer.putInt(intArray[index]));

        return byteBuffer.array();
    }

    /**
     * 将{@code byte[]}转换为{@code int[]}。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static int[] byteArrayToIntArray(byte[] byteArray) {
        assert (byteArray.length > 0 && byteArray.length % Integer.BYTES == 0);
        /*
         * 不能用ByteBuffer.warp(byteArray).asIntBuffer().array()操作，因为此时的IntBuffer是readOnly的，无法array()。
         * 尝试使用了Unsafe技术进行快速类型转换，实验表明：
         * 1. Unsafe在拷贝前还需要先把每个int对应的byte[0],byte[1],byte[2],byte[3]转换为byte[3],byte[2],byte[1],byte[0]
         * 2. 即便如此，这里仍然涉及内存拷贝，实际测试性能甚至比下述转换方法更慢
         */
        int[] intArray = new int[byteArray.length / Integer.BYTES];
        IntBuffer intBuffer = ByteBuffer.wrap(byteArray).asIntBuffer();
        IntStream.range(0, intBuffer.capacity()).forEach(index -> intArray[index] = intBuffer.get());

        return intArray;
    }
}

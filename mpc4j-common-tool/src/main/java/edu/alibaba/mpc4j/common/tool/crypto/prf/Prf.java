package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory.PrfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

import java.nio.ByteBuffer;

/**
 * 伪随机函数（Pseudo-random Function）接口。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public interface Prf {

    /**
     * 返回输出字节长度。
     *
     * @return 输出字节长度。
     */
    int getOutputByteLength();

    /**
     * 设置密钥。密钥将被拷贝，以放置后续可能的修改。
     *
     * @param key 密钥。
     */
    void setKey(byte[] key);

    /**
     * 返回密钥。
     *
     * @return 如果已设置密钥，则返回密钥；否则，返回null。
     */
    byte[] getKey();

    /**
     * 返回给定消息所对应的随机结果。
     *
     * @param message 消息。
     * @return 随机结果。
     */
    byte[] getBytes(byte[] message);

    /**
     * 返回给定消息所对应的布尔值。
     *
     * @param message 消息。
     * @return 布尔值。
     */
    default boolean getBoolean(byte[] message) {
        byte[] outputByteArray = this.getBytes(message);
        // 只采样{0, 1}，返回是否为0或者1
        return BinaryUtils.getBoolean(outputByteArray, outputByteArray.length - 1);
    }

    /**
     * 返回给定消息对应[0, bound)的整数值。
     *
     * @param message    消息。
     * @param upperBound 上界。
     * @return 范围为[0, range)的整数值。
     */
    default int getInteger(byte[] message, int upperBound) {
        assert upperBound > 0;
        assert getOutputByteLength() >= Integer.BYTES;
        if (upperBound == 1) {
            // 如果上界为1，则不用随机取值，直接返回0
            return 0;
        }
        byte[] byteArray = getBytes(message);
        return Math.abs(ByteBuffer.wrap(byteArray).getInt(byteArray.length - Integer.BYTES) % upperBound);
    }

    /**
     * 返回给定消息对应[0, bound)的整数值。
     *
     * @param index      索引值。
     * @param message    消息。
     * @param upperBound 上界。
     * @return 范围为[0, range)的整数值。
     */
    default int getInteger(int index, byte[] message, int upperBound) {
        assert index >= 0;
        byte[] indexMessage = ByteBuffer.allocate(message.length + Integer.BYTES)
            .putInt(index)
            .put(message)
            .array();
        return getInteger(indexMessage, upperBound);
    }

    /**
     * 返回给定消息对应[lowerBound, upperBound)的整数值。
     *
     * @param message    消息。
     * @param lowerBound 下界。
     * @param upperBound 上界。
     * @return 范围为[lowerBound, upperBound)的整数值。
     */
    default int getInteger(byte[] message, int lowerBound, int upperBound) {
        assert lowerBound < upperBound;
        return getInteger(message, upperBound - lowerBound) + lowerBound;
    }

    /**
     * 返回给定消息对应[0, bound)的长整数值。
     *
     * @param message    消息。
     * @param upperBound 上界。
     * @return 范围为[0, range)的长整数值。
     */
    default long getLong(byte[] message, long upperBound) {
        assert upperBound > 0;
        assert getOutputByteLength() >= Long.BYTES;
        if (upperBound == 1) {
            // 如果上界为1，则不用随机取值，直接返回0
            return 0;
        }
        byte[] byteArray = getBytes(message);
        return Math.abs(ByteBuffer.wrap(byteArray).getLong(byteArray.length - Long.BYTES) % upperBound);
    }

    /**
     * 返回给定消息对应[0, bound)的长整数值。
     *
     * @param index      索引值。
     * @param message    消息。
     * @param upperBound 上界。
     * @return 范围为[0, range)的长整数值。
     */
    default long getLong(int index, byte[] message, long upperBound) {
        assert index >= 0;
        byte[] indexMessage = ByteBuffer.allocate(message.length + Integer.BYTES)
            .putInt(index)
            .put(message)
            .array();
        return getLong(indexMessage, upperBound);
    }

    /**
     * 返回给定消息对应[0, 1)的浮点数。
     *
     * @param message 消息。
     * @return 浮点数。
     */
    default double getDouble(byte[] message) {
        return ((double)getInteger(message, Integer.MAX_VALUE)) / Integer.MAX_VALUE;
    }

    /**
     * 返回伪随机函数类型。
     *
     * @return 伪随机函数类型。
     */
    PrfType getPrfType();
}

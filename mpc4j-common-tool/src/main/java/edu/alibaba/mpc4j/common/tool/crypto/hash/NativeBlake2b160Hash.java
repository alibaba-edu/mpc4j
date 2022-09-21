package edu.alibaba.mpc4j.common.tool.crypto.hash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;

import java.util.Arrays;

/**
 * 本地Blake2b160哈希函数。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
class NativeBlake2b160Hash implements Hash {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }
    /**
     * 单位输出长度
     */
    static final int DIGEST_BYTE_LENGTH = 20;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    NativeBlake2b160Hash(int outputByteLength) {
        assert outputByteLength > 0 && outputByteLength <= DIGEST_BYTE_LENGTH
            : "Output byte length must be in range (0, " + DIGEST_BYTE_LENGTH + "]";
        this.outputByteLength = outputByteLength;
    }

    @Override
    public byte[] digestToBytes(byte[] message) {
        assert message.length > 0 : "Message length must be greater than 0: " + message.length;
        if (outputByteLength == DIGEST_BYTE_LENGTH) {
            // 如果输出字节长度等于哈希函数单位输出长度，则只调用一次哈希函数，且可以避免一次数组拷贝
            return digest(message);
        } else {
            // 如果输出字节长度小于哈希函数单位输出长度，则只调用一次哈希函数，截断输出
            byte[] output = digest(message);
            return Arrays.copyOf(output, outputByteLength);
        }
    }

    /**
     * 调用本地函数计算Blake2b哈希函数。
     *
     * @param message 输入消息。
     * @return 哈希结果。
     */
    private native byte[] digest(byte[] message);

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public HashType getHashType() {
        return HashType.NATIVE_BLAKE_2B_160;
    }
}

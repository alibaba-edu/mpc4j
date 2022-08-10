package edu.alibaba.mpc4j.common.tool.crypto.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * JDK的SHA256哈希函数。
 *
 * @author Weiran Liu
 * @date 2021/12/01
 */
public class JdkSha256Hash implements Hash {
    /**
     * JDK的SHA256哈希函数算法名称
     */
    private static final String JDK_HASH_ALGORITHM_NAME = "SHA-256";
    /**
     * 单位输出长度
     */
    static final int UNIT_BYTE_LENGTH = 32;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    JdkSha256Hash(int outputByteLength) {
        assert outputByteLength > 0 && outputByteLength <= UNIT_BYTE_LENGTH;
        this.outputByteLength = outputByteLength;
    }

    @Override
    public byte[] digestToBytes(byte[] message) {
       assert message.length > 0;
        // 哈希函数不是线程安全的，因此每调用一次都需要创建一个新的哈希函数实例，保证线程安全性
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(JDK_HASH_ALGORITHM_NAME);
            if (outputByteLength == UNIT_BYTE_LENGTH) {
                // 如果输出字节长度等于哈希函数单位输出长度，则只调用一次哈希函数，且可以避免一次数组拷贝
                return messageDigest.digest(message);
            } else {
                // 如果输出字节长度小于哈希函数单位输出长度，则只调用一次哈希函数，截断输出
                byte[] output = messageDigest.digest(message);
                return Arrays.copyOf(output, outputByteLength);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK hash instance with invalid algorithm name.");
        }
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public HashFactory.HashType getHashType() {
        return HashFactory.HashType.JDK_SHA256;
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * JDK的哈希函数实现。
 *
 * @author Weiran Liu
 * @date 2022/9/14
 */
abstract class AbstractJdkHash implements Hash {
    /**
     * 哈希函数类型
     */
    private final HashFactory.HashType hashType;
    /**
     * JDK哈希函数算法名称
     */
    private final String jdkHashName;
    /**
     * 单位输出长度
     */
    private final int digestByteLength;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    AbstractJdkHash(HashFactory.HashType hashType, String jdkHashName, int digestByteLength, int outputByteLength) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(jdkHashName);
            // 如果可以创建成功，则对相应参数赋值
            this.hashType = hashType;
            this.jdkHashName = jdkHashName;
            assert digestByteLength == messageDigest.getDigestLength()
                : "Pre-defined digest byte length should be equal to: " + messageDigest.getDigestLength();
            this.digestByteLength = digestByteLength;
            assert outputByteLength > 0 && outputByteLength <= digestByteLength
                : "Output byte length must be in range (0, " + digestByteLength + "]";
            this.outputByteLength = outputByteLength;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK hash instance with invalid algorithm name.");
        }
    }

    @Override
    public byte[] digestToBytes(byte[] message) {
        assert message.length > 0 : "Message length must be greater than 0: " + message.length;
        // 哈希函数不是线程安全的，因此每调用一次都需要创建一个新的哈希函数实例，保证线程安全性
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(jdkHashName);
            if (outputByteLength == digestByteLength) {
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
        return hashType;
    }
}

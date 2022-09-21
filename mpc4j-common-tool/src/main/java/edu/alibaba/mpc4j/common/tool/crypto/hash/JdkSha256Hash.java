package edu.alibaba.mpc4j.common.tool.crypto.hash;

/**
 * JDK的SHA256哈希函数。
 *
 * @author Weiran Liu
 * @date 2021/12/01
 */
class JdkSha256Hash extends AbstractJdkHash {
    /**
     * JDK的SHA256哈希函数算法名称
     */
    private static final String JDK_HASH_NAME = "SHA-256";
    /**
     * 单位输出长度
     */
    static final int DIGEST_BYTE_LENGTH = 32;

    JdkSha256Hash(int outputByteLength) {
        super(HashFactory.HashType.JDK_SHA256, JDK_HASH_NAME, DIGEST_BYTE_LENGTH, outputByteLength);
    }
}

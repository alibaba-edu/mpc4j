package edu.alibaba.mpc4j.common.tool.crypto.stream;

/**
 * 流密码接口。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
public interface StreamCipher {
    /**
     * 加密。
     *
     * @param key       密钥。
     * @param iv        初始向量。
     * @param plaintext 明文。
     * @return 密文。
     */
    byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext);

    /**
     * 加密，密文前包含初始向量。
     *
     * @param key       密钥。
     * @param iv        初始向量。
     * @param plaintext 明文。
     * @return 初始向量+密文。
     */
    byte[] ivEncrypt(byte[] key, byte[] iv, byte[] plaintext);

    /**
     * 解密。
     *
     * @param key        密钥。
     * @param iv         初始向量。
     * @param ciphertext 密文。
     * @return 明文。
     */
    byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext);

    /**
     * 解密。
     *
     * @param key        密钥。
     * @param ciphertext 初始向量+密文。
     * @return 明文。
     */
    byte[] ivDecrypt(byte[] key, byte[] ciphertext);

    /**
     * 返回初始向量（IV）的字节长度。
     *
     * @return 初始向量（IV）的字节长度。
     */
    int ivByteLength();

    /**
     * 返回类型。
     *
     * @return 类型。
     */
    StreamCipherFactory.StreamCipherType getType();
}

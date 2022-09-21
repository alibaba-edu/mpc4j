package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberKeyPair;
import edu.alibaba.mpc4j.common.tool.crypto.kyber.params.KyberParams;

/**
 * Kyber引擎接口。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public interface KyberEngine {
    /**
     * 返回Kyber类型。
     *
     * @return Kyber类型。
     */
    KyberEngineFactory.KyberType getKyberType();

    /**
     * 返回公钥字节长度。
     *
     * @return 公钥字节长度。
     */
    int publicKeyByteLength();

    /**
     * 返回封装密钥字节长度。
     *
     * @return 明文字节长度。
     */
    default int keyByteLength() {
        return KyberParams.SYM_BYTES;
    }

    /**
     * Generates public / secret key pair for the public-key encryption scheme underlying Kyber.
     *
     * @return public / secret key pair.
     */
    KyberKeyPair generateKeyPair();

    /**
     * Generate a random public key t.
     *
     * @return random public key t.
     */
    byte[] randomPublicKey();

    /**
     * Key encapsulation. The resulting key would be written in the input key.
     *
     * @param key        encapsulated key.
     * @param publicKey  public key t.
     * @param matrixSeed matrix seed.
     * @return ciphertext.
     */
    byte[] encapsulate(byte[] key, byte[] publicKey, byte[] matrixSeed);

    /**
     * Key decapsulation.
     *
     * @param ciphertext ciphertext.
     * @param secretKey  secret key s.
     * @param publicKey  public key t.
     * @param matrixSeed matrix seed.
     * @return encapsulated key.
     */
    byte[] decapsulate(byte[] ciphertext, short[][] secretKey, byte[] publicKey, byte[] matrixSeed);
}

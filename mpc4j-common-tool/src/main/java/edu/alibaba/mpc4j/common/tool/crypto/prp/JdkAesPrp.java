package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * JDK的AES伪随机置换。JDK会自动检查CPU是否支持AES-NI并自动化调用。此外，doFinal函数是线程安全的。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
class JdkAesPrp implements Prp {
    /**
     * JDK无填充AES-ECB模式名称
     */
    private static final String JDK_AES_MODE_NAME = "AES/ECB/NoPadding";
    /**
     * JDK的AES算法名称
     */
    private static final String JDK_AES_ALGORITHM_NAME = "AES";
    /**
     * 加密算法
     */
    private Cipher encryptCipher;
    /**
     * 解密算法
     */
    private Cipher decryptCipher;

    JdkAesPrp() {
        // empty
    }

    @Override
    public void setKey(byte[] key) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        try {
            // SecretKeySpec的JavaDoc称：The contents of the array are copied to protect against subsequent modification.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, JDK_AES_ALGORITHM_NAME);
            encryptCipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            decryptCipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid AES key length: %s bytes", key.length));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        assert encryptCipher != null;
        assert plaintext.length == CommonConstants.BLOCK_BYTE_LENGTH;
        try {
            // 不用检查明文长度，因为置换时会自动检测明文长度
            return this.encryptCipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(String.format("Invalid plaintext length: %s bytes", plaintext.length));
        }
    }

    @Override
    public byte[] invPrp(byte[] ciphertext) {
        assert decryptCipher != null;
        assert ciphertext.length == CommonConstants.BLOCK_BYTE_LENGTH;
        try {
            // 不用检查明文长度，因为置换时会自动检测明文长度
            return this.decryptCipher.doFinal(ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(String.format("Invalid ciphertext length: %s bytes", ciphertext.length));
        }
    }

    @Override
    public PrpFactory.PrpType getPrpType() {
        return PrpFactory.PrpType.JDK_AES;
    }
}

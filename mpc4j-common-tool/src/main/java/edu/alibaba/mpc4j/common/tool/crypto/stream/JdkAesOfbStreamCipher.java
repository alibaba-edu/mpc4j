package edu.alibaba.mpc4j.common.tool.crypto.stream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * JDK实现的AES-OFB流密码。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
class JdkAesOfbStreamCipher implements StreamCipher {
    /**
     * JDK的AES/OFB模式名称
     */
    private static final String JDK_AES_MODE_NAME = "AES/OFB/NoPadding";
    /**
     * AES引擎名称
     */
    private static final String JDK_AES_ALGORITHM_NAME = "AES";
    /**
     * 初始向量（IV）字节长度
     */
    private static final int IV_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;


    @Override
    public byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        // 经过测试，AES/OFB模式不是线程安全的，每次扩展要创建新的实例
        try {
            Cipher cipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            Key secretKeySpec = new SecretKeySpec(key, JDK_AES_ALGORITHM_NAME);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            // 初始化AES/CTR/NoPadding
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(plaintext);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid key length: %s bytes", key.length));
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
            | NoSuchPaddingException | NoSuchAlgorithmException ignored) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public byte[] ivEncrypt(byte[] key, byte[] iv, byte[] plaintext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        // 经过测试，AES/OFB模式不是线程安全的，每次扩展要创建新的实例
        try {
            Cipher cipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            Key secretKeySpec = new SecretKeySpec(key, JDK_AES_ALGORITHM_NAME);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            // 初始化AES/CTR/NoPadding
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            // 先拷贝IV
            byte[] ciphertext = new byte[IV_BYTE_LENGTH + plaintext.length];
            System.arraycopy(iv, 0, ciphertext, 0, IV_BYTE_LENGTH);
            // 再加密
            cipher.doFinal(plaintext, 0, plaintext.length, ciphertext, IV_BYTE_LENGTH);
            return ciphertext;
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid key length: %s bytes", key.length));
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
            | NoSuchPaddingException | NoSuchAlgorithmException | ShortBufferException ignored) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        assert ciphertext.length > 0;
        // 经过测试，AES/OFB模式不是线程安全的，每次扩展要创建新的实例
        try {
            Cipher cipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            Key secretKeySpec = new SecretKeySpec(key, JDK_AES_ALGORITHM_NAME);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            // 初始化AES/CTR/NoPadding
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(ciphertext);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid key length: %s bytes", key.length));
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
            | NoSuchPaddingException | NoSuchAlgorithmException ignored) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public byte[] ivDecrypt(byte[] key, byte[] ciphertext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert ciphertext.length > IV_BYTE_LENGTH;
        // 将密文拆分为IV和负载
        int plaintextByteLength = ciphertext.length - IV_BYTE_LENGTH;
        byte[] iv = new byte[IV_BYTE_LENGTH];
        System.arraycopy(ciphertext, 0, iv, 0, IV_BYTE_LENGTH);
        // 经过测试，AES/OFB模式不是线程安全的，每次扩展要创建新的实例
        try {
            Cipher cipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            Key secretKeySpec = new SecretKeySpec(key, JDK_AES_ALGORITHM_NAME);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            // 初始化AES/CTR/NoPadding
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            // 解密
            byte[] plaintext = new byte[plaintextByteLength];
            cipher.doFinal(ciphertext, IV_BYTE_LENGTH, plaintext.length, plaintext, 0);
            return plaintext;
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid key length: %s bytes", key.length));
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
            | NoSuchPaddingException | NoSuchAlgorithmException | ShortBufferException ignored) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public int ivByteLength() {
        return IV_BYTE_LENGTH;
    }

    @Override
    public StreamCipherFactory.StreamCipherType getType() {
        return StreamCipherFactory.StreamCipherType.JDK_AES_OFB;
    }
}

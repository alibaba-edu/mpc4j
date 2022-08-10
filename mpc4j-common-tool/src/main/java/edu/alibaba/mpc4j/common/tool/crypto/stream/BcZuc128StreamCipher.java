package edu.alibaba.mpc4j.common.tool.crypto.stream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.Zuc128Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Bouncy Castle实现的ZUC-128
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
class BcZuc128StreamCipher implements StreamCipher {
    /**
     * 初始向量（IV）字节长度
     */
    private static final int IV_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;

    BcZuc128StreamCipher() {

    }

    @Override
    public byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        KeyParameter keyParameter = new KeyParameter(key);
        CipherParameters cipherParameters = new ParametersWithIV(keyParameter, iv);
        Zuc128Engine streamCipher = new Zuc128Engine();
        streamCipher.init(true, cipherParameters);
        byte[] ciphertext = new byte[plaintext.length];
        streamCipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        return ciphertext;
    }

    @Override
    public byte[] ivEncrypt(byte[] key, byte[] iv, byte[] plaintext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        KeyParameter keyParameter = new KeyParameter(key);
        CipherParameters cipherParameters = new ParametersWithIV(keyParameter, iv);
        Zuc128Engine streamCipher = new Zuc128Engine();
        streamCipher.init(true, cipherParameters);
        // 先拷贝IV
        byte[] ciphertext = new byte[IV_BYTE_LENGTH + plaintext.length];
        System.arraycopy(iv, 0, ciphertext, 0, IV_BYTE_LENGTH);
        // 再加密
        streamCipher.processBytes(plaintext, 0, plaintext.length, ciphertext, IV_BYTE_LENGTH);
        return ciphertext;
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert iv.length == IV_BYTE_LENGTH;
        assert ciphertext.length > 0;
        KeyParameter keyParameter = new KeyParameter(key);
        CipherParameters cipherParameters = new ParametersWithIV(keyParameter, iv);
        Zuc128Engine streamCipher = new Zuc128Engine();
        streamCipher.init(false, cipherParameters);
        byte[] plaintext = new byte[ciphertext.length];
        streamCipher.processBytes(ciphertext, 0, ciphertext.length, plaintext, 0);
        return plaintext;
    }

    @Override
    public byte[] ivDecrypt(byte[] key, byte[] ciphertext) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert ciphertext.length > IV_BYTE_LENGTH;
        // 将密文拆分为IV和负载
        int payloadByteLength = ciphertext.length - IV_BYTE_LENGTH;
        byte[] iv = new byte[IV_BYTE_LENGTH];
        System.arraycopy(ciphertext, 0, iv, 0, IV_BYTE_LENGTH);
        KeyParameter keyParameter = new KeyParameter(key);
        CipherParameters cipherParameters = new ParametersWithIV(keyParameter, iv);
        Zuc128Engine streamCipher = new Zuc128Engine();
        streamCipher.init(false, cipherParameters);
        byte[] plaintext = new byte[payloadByteLength];
        streamCipher.processBytes(ciphertext, IV_BYTE_LENGTH, payloadByteLength, plaintext, 0);
        return plaintext;
    }

    @Override
    public int ivByteLength() {
        return IV_BYTE_LENGTH;
    }

    @Override
    public StreamCipherFactory.StreamCipherType getType() {
        return StreamCipherFactory.StreamCipherType.BC_ZUC_128;
    }
}

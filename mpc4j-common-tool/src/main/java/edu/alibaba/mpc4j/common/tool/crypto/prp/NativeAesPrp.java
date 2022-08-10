package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * 本地ECB-AES实现的伪随机置换。
 *
 * @author Weiran Liu
 * @date 2022/4/18
 */
public class NativeAesPrp implements Prp, Closeable {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 本地密钥指针
     */
    private ByteBuffer keyPointer;

    NativeAesPrp() {
        // empty
    }

    /**
     * 本地创建密钥。
     *
     * @param keyBytes 密钥。
     * @return 本地密钥指针。
     */
    private native ByteBuffer nativeSetKey(byte[] keyBytes);

    /**
     * 本地加密。
     *
     * @param keyPointer 本地密钥指针。
     * @param plaintext 明文。
     * @return 密文。
     */
    private native byte[] nativeEncrypt(ByteBuffer keyPointer, byte[] plaintext);

    /**
     * 本地解密。
     *
     * @param keyPointer 本地密钥指针。
     * @param ciphertext 密文。
     * @return 明文。
     */
    private native byte[] nativeDecrypt(ByteBuffer keyPointer, byte[] ciphertext);

    /**
     * 本地销毁密钥。
     *
     * @param keyPointer 本地密钥指针。
     */
    private native void nativeDestroyKey(ByteBuffer keyPointer);

    @Override
    public void setKey(byte[] key) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "key byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH;
        if (keyPointer != null) {
            nativeDestroyKey(keyPointer);
            keyPointer = null;
        }
        keyPointer = nativeSetKey(key);
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        assert keyPointer != null : "Please set key before encryption";
        assert plaintext.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "plaintext byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH;
        return nativeEncrypt(keyPointer, plaintext);
    }

    @Override
    public byte[] invPrp(byte[] ciphertext) {
        assert keyPointer != null : "Please set key before encryption";
        assert ciphertext.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "ciphertext byte length must be " + CommonConstants.BLOCK_BYTE_LENGTH;
        return nativeDecrypt(keyPointer, ciphertext);
    }

    @Override
    public PrpType getPrpType() {
        return PrpType.NATIVE_AES;
    }

    @Override
    public void close() {
        if (keyPointer != null) {
            nativeDestroyKey(keyPointer);
            keyPointer = null;
        }
    }
}

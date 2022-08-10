package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * 应用JDK自带的AES/CTR模式实现伪随机数生成器。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public class JdkAesCtrPrg implements Prg {
    /**
     * JDK的AES/CTR模式名称
     */
    private static final String JDK_AES_MODE_NAME = "AES/CTR/NoPadding";
    /**
     * AES引擎名称
     */
    private static final String JDK_AES_ALGORITHM_NAME = "AES";
    /**
     * 初始向量为全0
     */
    private static final IvParameterSpec IV = new IvParameterSpec(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    JdkAesCtrPrg(int outputByteLength) {
        this.outputByteLength = outputByteLength;
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public byte[] extendToBytes(byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 经过测试，AES/CTR模式不是线程安全的，每次扩展要创建新的实例
        try {
            Cipher cipher = Cipher.getInstance(JDK_AES_MODE_NAME);
            Key keySpec = new SecretKeySpec(seed, JDK_AES_ALGORITHM_NAME);
            // 初始化AES/CTR/NoPadding
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IV);
            // PRG加密的是一个全零的明文
            byte[] plaintext = new byte[outputByteLength];

            return cipher.doFinal(plaintext);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Invalid seed length: %s bytes", seed.length));
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
            | NoSuchPaddingException | NoSuchAlgorithmException ignored) {
            throw new IllegalStateException("System does not support " + JDK_AES_MODE_NAME);
        }
    }

    @Override
    public PrgFactory.PrgType getPrgType() {
        return PrgFactory.PrgType.JDK_AES_CTR;
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.engine.JdkSm4Engine;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;

/**
 * Bouncy Castle实现的SM4算法，对SM4算法实现进行了修改，使之变为了线程安全的实现。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
public class BcSm4Prp implements Prp {
    /**
     * 加密算法
     */
    private JdkSm4Engine encryptCipher;
    /**
     * 解密算法
     */
    private JdkSm4Engine decryptCipher;

    BcSm4Prp() {

    }

    @Override
    public void setKey(byte[] key) {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // JdkSm4Engine扩展密钥时会将密钥转换为int[]，此时已经发生了拷贝行为
        encryptCipher = new JdkSm4Engine();
        encryptCipher.init(true, key);
        decryptCipher = new JdkSm4Engine();
        decryptCipher.init(false, key);
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        assert encryptCipher != null;
        assert plaintext.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return encryptCipher.doFinal(plaintext);
    }

    @Override
    public byte[] invPrp(byte[] ciphertext) {
        assert decryptCipher != null;
        assert ciphertext.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return decryptCipher.doFinal(ciphertext);
    }

    @Override
    public PrpType getPrpType() {
        return PrpType.BC_SM4;
    }
}

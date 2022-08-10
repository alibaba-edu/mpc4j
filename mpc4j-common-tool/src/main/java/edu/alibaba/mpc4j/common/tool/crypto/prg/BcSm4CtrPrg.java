package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory.PrgType;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * 使用Bouncy Castle的SM4/CTR模式实现的伪随机数生成器。
 *
 * @author Weiran Liu
 * @date 2021/12/07
 */
public class BcSm4CtrPrg implements Prg {
    /**
     * 初始向量为全0
     */
    private static final byte[] IV = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    BcSm4CtrPrg(int outputByteLength) {
        this.outputByteLength = outputByteLength;
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public byte[] extendToBytes(byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 经过测试，SM4/CTR模式不是线程安全的，每次扩展要创建新的实例
        try {
            KeyParameter keyParameter = new KeyParameter(seed);
            ParametersWithIV parametersWithIv = new ParametersWithIV(keyParameter, IV);
            // 初始化SM4/CTR引擎
            BufferedBlockCipher sm4CtrCipher = new BufferedBlockCipher(new SICBlockCipher(new SM4Engine()));
            sm4CtrCipher.init(true, parametersWithIv);
            // PRG加密的是一个全零的明文
            byte[] plaintext = new byte[outputByteLength];
            byte[] ciphertext = new byte[outputByteLength];
            int offset = sm4CtrCipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
            sm4CtrCipher.doFinal(ciphertext, offset);

            return ciphertext;
        } catch (InvalidCipherTextException e) {
            throw new IllegalStateException(String.format("Invalid seed length: %s bytes", seed.length));
        }
    }

    @Override
    public PrgType getPrgType() {
        return PrgType.BC_SM4_CTR;
    }
}

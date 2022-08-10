package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 应用JDK内置的SHA1PRNG实现伪随机数生成器。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public class JdkSecureRandomPrg implements Prg {
    /**
     * JDK中只有SHA1PRNG支持固定种子伪随机数生成
     */
    private static final String JDK_SECURE_RANDOM_PRG_NAME = "SHA1PRNG";
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    JdkSecureRandomPrg(int outputByteLength) {
        this.outputByteLength = outputByteLength;
    }

    @Override
    public int getOutputByteLength() {
        return this.outputByteLength;
    }

    @Override
    public byte[] extendToBytes(byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        try {
            // 每次要重新创建一个新的SecureRandom，否则即使是相同的密钥，输出结果也不同
            SecureRandom secureRandom = SecureRandom.getInstance(JDK_SECURE_RANDOM_PRG_NAME);
            secureRandom.setSeed(seed);
            byte[] outputByteArray = new byte[outputByteLength];
            secureRandom.nextBytes(outputByteArray);

            return outputByteArray;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK hash instance with invalid algorithm name.");
        }
    }

    @Override
    public PrgFactory.PrgType getPrgType() {
        return PrgFactory.PrgType.JDK_SECURE_RANDOM;
    }
}

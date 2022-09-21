package edu.alibaba.mpc4j.common.tool.crypto.hash;

import org.bouncycastle.crypto.digests.SHAKEDigest;

/**
 * 用Bouncy Castle实现的Shake128哈希函数。
 *
 * @author Weiran Liu
 * @date 2022/9/14
 */
class BcShake256Hash implements Hash {
    /**
     * 输出字节长度
     */
    private final int outputByteLength;

    BcShake256Hash(int outputByteLength) {
        assert outputByteLength > 0 : "Output byte length must be greater than 0: " + outputByteLength;
        this.outputByteLength = outputByteLength;
    }

    @Override
    public byte[] digestToBytes(byte[] message) {
        assert message.length > 0 : "Message length must be greater than 0: " + message.length;
        // 哈希函数不是线程安全的，因此每调用一次都需要创建一个新的哈希函数实例，保证线程安全性
        SHAKEDigest shakeDigest = new SHAKEDigest(256);
        byte[] output = new byte[outputByteLength];
        shakeDigest.update(message, 0, message.length);
        shakeDigest.doFinal(output, 0, outputByteLength);

        return output;
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public HashFactory.HashType getHashType() {
        return HashFactory.HashType.BC_SHAKE_256;
    }
}

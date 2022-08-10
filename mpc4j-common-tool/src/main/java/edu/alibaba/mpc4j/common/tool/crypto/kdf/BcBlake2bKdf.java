package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;
import org.bouncycastle.crypto.digests.Blake2bDigest;

/**
 * Bouncy Castle实现的Blake2b密钥派生函数。
 * 实现思路来自于https://github.com/ladnir/cryptoTools/blob/master/cryptoTools/Crypto/Blake2.h
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
class BcBlake2bKdf implements Kdf {

    @Override
    public byte[] deriveKey(byte[] seed) {
        assert seed.length > 0;
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // 哈希函数不是线程安全的，因此每调用一次都需要创建一个新的哈希函数实例，保证线程安全性
        Blake2bDigest blake2bDigest = new Blake2bDigest(CommonConstants.BLOCK_BIT_LENGTH);
        blake2bDigest.update(seed, 0, seed.length);
        blake2bDigest.doFinal(key, 0);

        return key;
    }

    @Override
    public KdfType getKdfType() {
        return KdfType.BC_BLAKE_2B;
    }
}

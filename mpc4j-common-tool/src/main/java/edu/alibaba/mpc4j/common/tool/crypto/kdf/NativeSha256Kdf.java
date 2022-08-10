package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;

/**
 * 应用本地SHA256哈希实现密钥派生函数。
 * 代码参考：https://github.com/emp-toolkit/emp-tool/blob/master/emp-tool/utils/hash.h中的KDF函数。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
class NativeSha256Kdf implements Kdf {
    /**
     * 哈希函数
     */
    private final Hash hash;

    NativeSha256Kdf() {
        hash = HashFactory.createInstance(HashType.NATIVE_SHA256, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public byte[] deriveKey(byte[] seed) {
        return hash.digestToBytes(seed);
    }

    @Override
    public KdfType getKdfType() {
        return KdfType.NATIVE_SHA256;
    }
}

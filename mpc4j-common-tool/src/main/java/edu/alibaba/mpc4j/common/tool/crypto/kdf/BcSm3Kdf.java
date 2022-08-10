package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;

/**
 * 应用Bouncy Castle的SM3哈希实现密钥派生函数。
 * 代码参考：https://github.com/emp-toolkit/emp-tool/blob/master/emp-tool/utils/hash.h中的KDF函数。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
class BcSm3Kdf implements Kdf {
    /**
     * 哈希函数
     */
    private final Hash hash;

    BcSm3Kdf() {
        hash = HashFactory.createInstance(HashType.BC_SM3, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public byte[] deriveKey(byte[] seed) {
        return hash.digestToBytes(seed);
    }

    @Override
    public KdfType getKdfType() {
        return KdfType.BC_SM3;
    }
}

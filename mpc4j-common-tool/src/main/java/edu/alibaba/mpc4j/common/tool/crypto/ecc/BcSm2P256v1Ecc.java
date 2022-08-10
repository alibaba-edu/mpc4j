package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 用Bouncy Castle实现的SM2椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
class BcSm2P256v1Ecc extends AbstractBcEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    BcSm2P256v1Ecc() {
        super(EccFactory.EccType.BC_SM2_P256_V1, "sm2p256v1");
        // 初始化哈希函数，国产椭圆曲线，使用SM3
        hash = HashFactory.createInstance(HashFactory.HashType.BC_SM3, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

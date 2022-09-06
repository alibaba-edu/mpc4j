package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 用Bouncy Castle实现的SM2椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
public class Sm2P256v1BcEcc extends AbstractEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    public Sm2P256v1BcEcc() {
        super(EccFactory.EccType.SM2_P256_V1_BC, "sm2p256v1");
        // 初始化哈希函数，国产椭圆曲线，使用SM3
        hash = HashFactory.createInstance(HashFactory.HashType.BC_SM3, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

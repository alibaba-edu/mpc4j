package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 应用Bouncy Castle实现的SecP256k1。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
class BcSecP256k1Ecc extends AbstractBcEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    BcSecP256k1Ecc() {
        super(EccFactory.EccType.BC_SEC_P256_K1, "secp256k1");
        // 初始化哈希函数，为与MCL兼容，必须使用SHA256
        hash = HashFactory.createInstance(HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

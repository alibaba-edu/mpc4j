package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 应用Bouncy Castle实现的SecP256r1。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class BcSecP256r1Ecc extends AbstractBcEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    BcSecP256r1Ecc() {
        super(EccFactory.EccType.BC_SEC_P256_R1, "secp256r1");
        // 初始化哈希函数，为与MCL兼容，必须使用SHA256
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

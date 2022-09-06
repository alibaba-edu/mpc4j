package edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 调用MCL库实现的SecP256k1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
public class SecP256k1MclEcc extends AbstractMclEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    public SecP256k1MclEcc() {
        super(SecP256k1MclNativeEcc.getInstance(), EccFactory.EccType.SEC_P256_K1_MCL, "secp256k1");
        // 初始化哈希函数
        hash = HashFactory.createInstance(HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

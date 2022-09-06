package edu.alibaba.mpc4j.common.tool.crypto.ecc.mcl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 调用MCL库实现的SecP256r1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class SecP256r1MclEcc extends AbstractMclEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    public SecP256r1MclEcc() {
        super(SecP256r1MclNativeEcc.getInstance(), EccFactory.EccType.SEC_P256_R1_MCL, "secp256r1");
        // 初始化哈希函数
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

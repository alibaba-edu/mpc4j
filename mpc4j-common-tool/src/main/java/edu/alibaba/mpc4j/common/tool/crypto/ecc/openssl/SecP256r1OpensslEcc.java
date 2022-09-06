package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 调用OpenSSL库实现的SecP256r1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/9/2
 */
public class SecP256r1OpensslEcc extends AbstractOpensslEcc {
    /**
     * 哈希到椭圆曲线所用的哈希函数
     */
    private final Hash hash;

    public SecP256r1OpensslEcc() {
        super(SecP256r1OpensslNativeEcc.getInstance(), EccFactory.EccType.SEC_P256_R1_OPENSSL, "secp256r1");
        // 初始化哈希函数
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] message) {
        return hashToCurve(message, hash);
    }
}

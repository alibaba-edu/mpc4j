package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 调用OpenSSL库实现的SecP256r1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public class Sm2P256v1OpensslEcc extends AbstractOpensslEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private Sm2P256v1OpensslEcc() {
        super(Sm2P256v1OpensslNativeEcc.getInstance(), EccFactory.EccType.SM2_P256_V1_OPENSSL, "sm2p256v1");
        // initialize the hash function with SM3
        hash = HashFactory.createInstance(HashFactory.HashType.BC_SM3, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final Sm2P256v1OpensslEcc INSTANCE = new Sm2P256v1OpensslEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Sm2P256v1OpensslEcc getInstance() {
        return INSTANCE;
    }
}

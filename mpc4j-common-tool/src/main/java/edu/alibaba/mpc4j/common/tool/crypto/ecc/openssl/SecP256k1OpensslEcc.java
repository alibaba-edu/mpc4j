package edu.alibaba.mpc4j.common.tool.crypto.ecc.openssl;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 调用OpenSSL库实现的SecP256k1椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public class SecP256k1OpensslEcc extends AbstractOpensslEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private SecP256k1OpensslEcc() {
        super(SecP256k1OpensslNativeEcc.getInstance(), EccFactory.EccType.SEC_P256_K1_OPENSSL, "secp256k1");
        // initialize the hash function with SHA256, same as in MCL
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final SecP256k1OpensslEcc INSTANCE = new SecP256k1OpensslEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static SecP256k1OpensslEcc getInstance() {
        return INSTANCE;
    }
}

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
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private SecP256r1OpensslEcc() {
        super(SecP256r1OpensslNativeEcc.getInstance(), EccFactory.EccType.SEC_P256_R1_OPENSSL, "secp256r1");
        // initialize the hash function with SHA256
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final SecP256r1OpensslEcc INSTANCE = new SecP256r1OpensslEcc();

    /**
     * Gets the instance.
     *
     * @return instance.
     */
    public static SecP256r1OpensslEcc getInstance() {
        return INSTANCE;
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 应用Bouncy Castle实现的SecP256r1。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public class SecP256r1BcEcc extends AbstractEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private SecP256r1BcEcc() {
        super(EccFactory.EccType.SEC_P256_R1_BC, "secp256r1");
        // initialize the hash function with SHA256, same as in OpenSSL
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final SecP256r1BcEcc INSTANCE = new SecP256r1BcEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static SecP256r1BcEcc getInstance() {
        return INSTANCE;
    }
}

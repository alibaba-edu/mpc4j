package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
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
public class SecP256k1BcEcc extends AbstractEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private SecP256k1BcEcc() {
        super(EccFactory.EccType.SEC_P256_K1_BC, "secp256k1");
        // initialize the hash function with SHA256, same as in MCL
        hash = HashFactory.createInstance(HashType.JDK_SHA256, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final SecP256k1BcEcc INSTANCE = new SecP256k1BcEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static SecP256k1BcEcc getInstance() {
        return INSTANCE;
    }
}

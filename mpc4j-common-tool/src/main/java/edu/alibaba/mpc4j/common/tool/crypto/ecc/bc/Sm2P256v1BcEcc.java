package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 用Bouncy Castle实现的SM2椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
public class Sm2P256v1BcEcc extends AbstractEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private Sm2P256v1BcEcc() {
        super(EccFactory.EccType.SM2_P256_V1_BC, "sm2p256v1");
        // initialize the hash function with SM3, same as in OpenSSL
        hash = HashFactory.createInstance(HashFactory.HashType.BC_SM3, 32);
    }

    @Override
    public ECPoint hashToCurve(byte[] data) {
        return hashToCurve(data, hash);
    }

    /**
     * singleton mode
     */
    private static final Sm2P256v1BcEcc INSTANCE = new Sm2P256v1BcEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Sm2P256v1BcEcc getInstance() {
        return INSTANCE;
    }
}

package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.AbstractEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.bouncycastle.math.ec.ECPoint;

/**
 * 应用Bouncy Castle实现的Curve25519。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
public class Curve25519BcEcc extends AbstractEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private Curve25519BcEcc() {
        super(EccFactory.EccType.CURVE25519_BC, "curve25519");
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
    private static final Curve25519BcEcc INSTANCE = new Curve25519BcEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Curve25519BcEcc getInstance() {
        return INSTANCE;
    }
}

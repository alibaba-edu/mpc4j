package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * RA17 ECC single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class Ra17EccSqOprfKey implements SqOprfKey {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * α
     */
    private final BigInteger alpha;

    Ra17EccSqOprfKey(EnvType envType, BigInteger alpha) {
        ecc = EccFactory.createInstance(envType);
        kdf = KdfFactory.createInstance(envType);
        this.alpha = alpha;
    }

    /**
     * Gets α.
     *
     * @return α.
     */
    public BigInteger getAlpha() {
        return alpha;
    }

    @Override
    public byte[] getPrf(byte[] input) {
        ECPoint output = ecc.hashToCurve(input);
        ECPoint prf = ecc.multiply(output, alpha);
        return kdf.deriveKey(ecc.encode(prf, false));
    }
}

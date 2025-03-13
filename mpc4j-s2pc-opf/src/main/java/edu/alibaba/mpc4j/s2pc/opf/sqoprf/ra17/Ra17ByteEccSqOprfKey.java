package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

import java.math.BigInteger;

/**
 * RA17 byte ECC single-query OPRF key.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class Ra17ByteEccSqOprfKey implements SqOprfKey {
    /**
     * byte full ECC
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * α
     */
    private final BigInteger alpha;

    Ra17ByteEccSqOprfKey(EnvType envType, BigInteger alpha) {
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
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
        byte[] output = byteFullEcc.hashToCurve(input);
        byte[] prf = byteFullEcc.mul(output, alpha);
        return kdf.deriveKey(prf);
    }
}

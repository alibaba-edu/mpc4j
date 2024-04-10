package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3BlazeGctDovsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H3BlazeGctEccDokvs<T> extends AbstractH3GctEccDokvs<T> {

    H3BlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H3BlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, ecc, n,
            H3BlazeGctDovsUtils.getLm(n), H3BlazeGctDovsUtils.getRm(n),
            keys, secureRandom
        );
    }

    @Override
    public EccDokvsType getType() {
        return EccDokvsType.H3_BLAZE_GCT;
    }
}

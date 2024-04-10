package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H3SingletonGctEccDokvs<T> extends AbstractH3GctEccDokvs<T> {

    H3SingletonGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H3SingletonGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, ecc, n,
            H3NaiveGctDokvsUtils.getLm(n), H3NaiveGctDokvsUtils.getRm(n),
            keys, secureRandom
        );
    }

    @Override
    public EccDokvsType getType() {
        return EccDokvsType.H3_SINGLETON_GCT;
    }
}

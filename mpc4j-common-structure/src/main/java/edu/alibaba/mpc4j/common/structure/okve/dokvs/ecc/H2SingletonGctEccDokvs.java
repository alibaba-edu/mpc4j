package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H2SingletonGctEccDokvs<T> extends AbstractH2GctEccDokvs<T> {

    H2SingletonGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H2SingletonGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, ecc, n,
            H2NaiveGctDokvsUtils.getLm(n), H2NaiveGctDokvsUtils.getRm(n),
            keys, new CuckooTableSingletonTcFinder<>(), secureRandom
        );
    }

    @Override
    public EccDokvsType getType() {
        return EccDokvsType.H2_SINGLETON_GCT;
    }
}

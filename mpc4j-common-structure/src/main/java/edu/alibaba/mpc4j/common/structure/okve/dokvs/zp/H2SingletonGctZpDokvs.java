package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/5
 */
class H2SingletonGctZpDokvs<T> extends AbstractH2GctZpDokvs<T> {

    H2SingletonGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H2SingletonGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, p, n,
            H2NaiveGctDokvsUtils.getLm(n), H2NaiveGctDokvsUtils.getRm(n),
            keys, new CuckooTableSingletonTcFinder<>(), secureRandom
        );
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H2_SINGLETON_GCT;
    }
}

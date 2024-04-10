package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/2/20
 */
class H2TwoCoreGctZpDokvs<T> extends AbstractH2GctZpDokvs<T> {

    H2TwoCoreGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H2TwoCoreGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, p, n,
            H2NaiveGctDokvsUtils.getLm(n), H2NaiveGctDokvsUtils.getRm(n),
            keys, new H2CuckooTableTcFinder<>(), secureRandom
        );
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H2_TWO_CORE_GCT;
    }
}

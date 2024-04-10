package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
class H3SingletonGctZpDokvs<T> extends AbstractH3GctZpDokvs<T> {

    H3SingletonGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H3SingletonGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, p, n,
            H3NaiveGctDokvsUtils.getLm(n), H3NaiveGctDokvsUtils.getRm(n),
            keys, secureRandom
        );
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H3_SINGLETON_GCT;
    }
}

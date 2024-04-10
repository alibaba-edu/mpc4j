package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2BlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/5
 */
class H2BlazeGctZpDokvs<T> extends AbstractH2GctZpDokvs<T> {

    H2BlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H2BlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, p, n,
            H2BlazeGctDokvsUtils.getLm(n), H2BlazeGctDokvsUtils.getRm(n),
            keys, new CuckooTableSingletonTcFinder<>(), secureRandom);
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H2_BLAZE_GCT;
    }
}

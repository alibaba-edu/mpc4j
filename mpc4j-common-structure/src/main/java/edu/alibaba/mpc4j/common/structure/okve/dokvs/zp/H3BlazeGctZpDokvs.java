package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3BlazeGctDovsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
class H3BlazeGctZpDokvs<T> extends AbstractH3GctZpDokvs<T> {

    H3BlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H3BlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, p, n,
            H3BlazeGctDovsUtils.getLm(n), H3BlazeGctDovsUtils.getRm(n),
            keys, secureRandom
        );
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H3_BLAZE_GCT;
    }
}

package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2BlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H2BlazeGctEccDokvs<T> extends AbstractH2GctEccDokvs<T> {

    H2BlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H2BlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, ecc, n,
            H2BlazeGctDokvsUtils.getLm(n), H2BlazeGctDokvsUtils.getRm(n),
            keys, new CuckooTableSingletonTcFinder<>(), secureRandom);
    }

    @Override
    public EccDokvsType getType() {
        return EccDokvsType.H2_BLAZE_GCT;
    }
}

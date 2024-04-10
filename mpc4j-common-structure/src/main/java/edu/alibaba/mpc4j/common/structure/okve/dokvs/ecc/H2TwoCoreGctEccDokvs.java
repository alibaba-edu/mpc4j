package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class H2TwoCoreGctEccDokvs<T> extends AbstractH2GctEccDokvs<T> {

    H2TwoCoreGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H2TwoCoreGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, ecc, n,
            H2NaiveGctDokvsUtils.getLm(n), H2NaiveGctDokvsUtils.getRm(n),
            keys, new H2CuckooTableTcFinder<>(), secureRandom
        );
    }

    @Override
    public EccDokvsType getType() {
        return EccDokvsType.H2_TWO_CORE_GCT;
    }
}

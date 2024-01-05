package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2BlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 2 hash functions. The construction is from the following paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H2BlazeGctGf2eDokvs<T> extends AbstractH2GctGf2eDokvs<T> {

    H2BlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H2BlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, n,
            H2BlazeGctDokvsUtils.getLm(n), H2BlazeGctDokvsUtils.getRm(n),
            l, keys, new CuckooTableSingletonTcFinder<>(), secureRandom);
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H2_BLAZE_GCT;
    }
}

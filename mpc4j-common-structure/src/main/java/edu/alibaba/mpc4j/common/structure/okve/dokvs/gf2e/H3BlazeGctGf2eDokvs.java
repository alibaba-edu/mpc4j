package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3BlazeGctDovsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * Blazing fast DOKVS using garbled cuckoo table with 3 hash functions. The construction is from the following paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H3BlazeGctGf2eDokvs<T> extends AbstractH3GctGf2eDokvs<T> {

    H3BlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H3BlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, n,
            H3BlazeGctDovsUtils.getLm(n), H3BlazeGctDovsUtils.getRm(n),
            l, keys, secureRandom
        );
    }

    @Override
    public Gf2eDokvsType getType() {
        return Gf2eDokvsType.H3_BLAZE_GCT;
    }
}

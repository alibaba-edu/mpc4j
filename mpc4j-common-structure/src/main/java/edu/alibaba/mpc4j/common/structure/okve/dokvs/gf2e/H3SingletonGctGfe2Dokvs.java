package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3NaiveGctDokvsUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 3 hash functions. The non-doubly construction is from the following paper:
 * <p>
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Zhang, Cong, Yu Chen, Weiran Liu, Min Zhang, and Dongdai Lin. Linear Private Set Union from Multi-Query Reverse
 * Private Membership Test. USENIX Security 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H3SingletonGctGfe2Dokvs<T> extends AbstractH3GctGf2eDokvs<T> {

    H3SingletonGctGfe2Dokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H3SingletonGctGfe2Dokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, n,
            H3NaiveGctDokvsUtils.getLm(n), H3NaiveGctDokvsUtils.getRm(n),
            l, keys, secureRandom
        );
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H3_SINGLETON_GCT;
    }
}

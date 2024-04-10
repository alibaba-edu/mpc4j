package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * Distinct Garbled Bloom Filter (GBF) DOKVS. The original scheme is described in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 * </p>
 * In this implementation, we require that any inputs have constant distinct positions in the Garbled Bloom Filter.
 * This requirement is used in the following paper:
 * <p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class DistinctGbfGf2eDokvs<T> extends AbstractGbfGf2eDokvs<T> {

    public DistinctGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key) {
        this(envType, n, l, key, new SecureRandom());
    }

    public DistinctGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key, SecureRandom secureRandom) {
        super(envType, n, l, key, secureRandom);
    }

    @Override
    public int[] sparsePositions(T key) {
        return DistinctGbfUtils.sparsePositions(hash, key, m);
    }

    @Override
    public int sparsePositionNum() {
        return DistinctGbfUtils.SPARSE_HASH_NUM;
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.DISTINCT_GBF;
    }
}

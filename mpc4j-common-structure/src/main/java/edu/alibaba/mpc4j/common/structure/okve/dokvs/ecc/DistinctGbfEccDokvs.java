package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;

import java.security.SecureRandom;

/**
 * Distinct Garbled Bloom Filter (GBF) DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
class DistinctGbfEccDokvs<T> extends AbstractGbfEccDokvs<T> {

    DistinctGbfEccDokvs(EnvType envType, Ecc ecc, int n, byte[] key) {
        this(envType, ecc, n, key, new SecureRandom());
    }

    DistinctGbfEccDokvs(EnvType envType, Ecc ecc, int n, byte[] key, SecureRandom secureRandom) {
        super(envType, ecc, n, key, secureRandom);
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
    public EccDokvsType getType() {
        return EccDokvsType.DISTINCT_GBF;
    }
}

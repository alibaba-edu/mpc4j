package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Distinct Garbled Bloom Filter (GBF) DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/2/19
 */
class DistinctGbfZpDokvs<T> extends AbstractGbfZpDokvs<T> {

    public DistinctGbfZpDokvs(EnvType envType, BigInteger p, int n, byte[] key) {
        this(envType, p, n, key, new SecureRandom());
    }

    public DistinctGbfZpDokvs(EnvType envType, BigInteger p, int n, byte[] key, SecureRandom secureRandom) {
        super(envType, p, n, key, secureRandom);
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
    public ZpDokvsType getType() {
        return ZpDokvsType.DISTINCT_GBF;
    }
}

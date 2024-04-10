package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * abstract Garbled Bloom Filter DOKVS. The original scheme is described in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 * </p>
 * The following paper points out that GBF is a DOKVS.
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
abstract class AbstractGbfGf2eDokvs<T> extends AbstractGf2eDokvs<T> implements SparseGf2eDokvs<T> {
    /**
     * hashes
     */
    protected final Prf hash;

    AbstractGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key, SecureRandom secureRandom) {
        super(n, DistinctGbfUtils.getM(n), l, secureRandom);
        hash = PrfFactory.createInstance(envType, Integer.BYTES * DistinctGbfUtils.SPARSE_HASH_NUM);
        hash.setKey(key);
    }

    @Override
    public int sparsePositionRange() {
        return m;
    }

    @Override
    public int sparsePositionNum() {
        return DistinctGbfUtils.SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] binaryDensePositions(T key) {
        // garbled bloom filter does not contain dense part
        return new boolean[0];
    }

    @Override
    public int densePositionRange() {
        return 0;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        keyValueMap.values().forEach(x -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(x, byteL, l)));
        Set<T> keySet = keyValueMap.keySet();
        Stream<T> keyStream = keySet.stream();
        keyStream = parallelEncode ? keyStream.parallel() : keyStream;
        Map<T, int[]> sparsePositionsMap = keyStream.collect(Collectors.toMap(key -> key, this::sparsePositions));
        // compute positions for all keys, create shares.
        byte[][] storage = new byte[m][];
        for (T key : keySet) {
            byte[] finalShare = BytesUtils.clone(keyValueMap.get(key));
            int[] sparsePositions = sparsePositionsMap.get(key);
            int emptySlot = -1;
            for (int position : sparsePositions) {
                if (storage[position] == null && emptySlot == -1) {
                    // if we find an empty position, reserve the location for finalShare）
                    emptySlot = position;
                } else if (storage[position] == null) {
                    // if the current position is null, generate a new share
                    storage[position] = BytesUtils.randomByteArray(byteL, l, secureRandom);
                    BytesUtils.xori(finalShare, storage[position]);
                } else {
                    // if the current position is not null, reuse the share
                    BytesUtils.xori(finalShare, storage[position]);
                }
            }
            if (emptySlot == -1) {
                // we cannot find an empty position, which happens with probability 1 - 2^{-λ}
                throw new ArithmeticException("Failed to encode Key-Value Map, cannot find empty slot");
            }
            storage[emptySlot] = finalShare;
        }
        // pad random elements in all empty positions.
        for (int i = 0; i < m; i++) {
            if (storage[i] == null) {
                storage[i] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }

        return storage;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        int[] sparsePositions = sparsePositions(key);
        byte[] value = new byte[byteL];
        for (int position : sparsePositions) {
            BytesUtils.xori(value, storage[position]);
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }
}

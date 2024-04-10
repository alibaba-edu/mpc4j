package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.DistinctGbfUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.bouncycastle.math.ec.ECPoint;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * abstract Garbled Bloom Filter DOKVS.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
abstract class AbstractGbfEccDokvs<T> extends AbstractEccDokvs<T> implements SparseEccDokvs<T> {
    /**
     * hashes
     */
    protected final Prf hash;

    AbstractGbfEccDokvs(EnvType envType, Ecc ecc, int n, byte[] key, SecureRandom secureRandom) {
        super(envType, ecc, n, DistinctGbfUtils.getM(n), secureRandom);
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
    public ECPoint[] encode(Map<T, ECPoint> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        Set<T> keySet = keyValueMap.keySet();
        Stream<T> keyStream = keySet.stream();
        keyStream = parallelEncode ? keyStream.parallel() : keyStream;
        Map<T, int[]> sparsePositionsMap = keyStream.collect(Collectors.toMap(key -> key, this::sparsePositions));
        // compute positions for all keys, create shares.
        ECPoint[] storage = new ECPoint[m];
        for (T key : keySet) {
            ECPoint finalShare = keyValueMap.get(key);
            int[] sparsePositions = sparsePositionsMap.get(key);
            int emptySlot = -1;
            for (int position : sparsePositions) {
                if (storage[position] == null && emptySlot == -1) {
                    // if we find an empty position, reserve the location for finalShare）
                    emptySlot = position;
                } else if (storage[position] == null) {
                    // if the current position is null, generate a new share
                    storage[position] = ecc.randomPoint(secureRandom);
                    finalShare = ecc.subtract(finalShare, storage[position]);
                } else {
                    // if the current position is not null, reuse the share
                    finalShare = ecc.subtract(finalShare, storage[position]);
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
                storage[i] = ecc.randomPoint(secureRandom);
            }
        }

        return storage;
    }

    @Override
    public ECPoint decode(ECPoint[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, getM());
        int[] sparsePositions = sparsePositions(key);
        ECPoint value = ecc.getInfinity();
        for (int position : sparsePositions) {
            value = ecc.add(value, storage[position]);
        }
        return value;
    }
}

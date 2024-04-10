package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2ClusterBlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Sparse clustering blazing fast DOKVS using garbled cuckoo table with 2 hash functions. We rearrange the storages
 * so that the dense part are clustered together.
 *
 * @author Weiran Liu
 * @date 2024/3/5
 */
class H2SparseClusterBlazeGctZpDokvs<T> extends AbstractH2ClusterBlazeGctZpDokvs<T> implements SparseZpDokvs<T> {

    H2SparseClusterBlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H2SparseClusterBlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(envType, p, n, keys, secureRandom);
    }

    @Override
    public int[] positions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binPositions = bins.get(binIndex).positions(key);
        return Arrays.stream(binPositions)
            .map(position -> position + binM * binIndex)
            .toArray();
    }

    @Override
    public int sparsePositionRange() {
        return binNum * binLm;
    }

    @Override
    public int sparsePositionNum() {
        return H2ClusterBlazeGctDokvsUtils.SPARSE_HASH_NUM;
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binSparsePositions = bins.get(binIndex).sparsePositions(key);
        return Arrays.stream(binSparsePositions)
            .map(position -> position + binLm * binIndex)
            .toArray();
    }

    @Override
    public boolean[] binaryDensePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        boolean[] binBinaryDensePositions = bins.get(binIndex).binaryDensePositions(key);
        boolean[] binaryDensePositions = new boolean[binNum * binRm];
        System.arraycopy(binBinaryDensePositions, 0, binaryDensePositions, binIndex * binRm, binRm);
        return binaryDensePositions;
    }

    @Override
    public int densePositionRange() {
        return binNum * binRm;
    }

    @Override
    public ZpDokvsType getType() {
        return ZpDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT;
    }

    @Override
    public BigInteger[] encode(Map<T, BigInteger> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        // create and split bins
        ArrayList<Map<T, BigInteger>> keyValueMaps = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ConcurrentHashMap<T, BigInteger>(binN))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<Entry<T, BigInteger>> keyValueStream = keyValueMap.entrySet().stream();
        keyValueStream = parallelEncode ? keyValueStream.parallel() : keyValueStream;
        keyValueStream.forEach(entry -> {
            byte[] keyByte = ObjectUtils.objectToByteArray(entry.getKey());
            int binIndex = binHash.getInteger(keyByte, binNum);
            keyValueMaps.get(binIndex).put(entry.getKey(), entry.getValue());
        });
        // encode
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        BigInteger[][] naiveStorage = binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .toArray(BigInteger[][]::new);
        // rearrange storage
        BigInteger[] sparseStorage = new BigInteger[binNum * binM];
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            // copy sparse positions
            System.arraycopy(naiveStorage[binIndex], 0, sparseStorage, binLm * binIndex, binLm);
            // copy dense positions
            System.arraycopy(naiveStorage[binIndex], binLm, sparseStorage, binLm * binNum + binRm * binIndex, binRm);
        }
        return sparseStorage;
    }

    @Override
    public BigInteger decode(BigInteger[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binSparsePositions = bins.get(binIndex).sparsePositions(key);
        boolean[] binDensePositions = bins.get(binIndex).binaryDensePositions(key);
        BigInteger value = zp.createZero();
        for (int binSparsePosition : binSparsePositions) {
            value = zp.add(value, storage[binLm * binIndex + binSparsePosition]);
        }
        for (int binDensePosition = 0; binDensePosition < binRm; binDensePosition++) {
            if (binDensePositions[binDensePosition]) {
                value = zp.add(value, storage[binLm * binNum + binRm * binIndex + binDensePosition]);
            }
        }
        return value;
    }
}

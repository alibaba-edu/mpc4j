package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3ClusterBlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Sparse clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions. We rearrange the storages
 * so that the dense part are clustered together.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
class H3SparseClusterBlazeGctGf2eDokvs<T> extends AbstractH3ClusterBlazeGctGf2eDokvs<T> implements SparseGf2eDokvs<T> {

    H3SparseClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H3SparseClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, l, keys, secureRandom);
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
        return H3ClusterBlazeGctDokvsUtils.SPARSE_HASH_NUM;
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
    public Gf2eDokvsType getType() {
        return Gf2eDokvsType.H3_SPARSE_CLUSTER_BLAZE_GCT;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        // create and split bins
        ArrayList<Map<T, byte[]>> keyValueMaps = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ConcurrentHashMap<T, byte[]>(binN))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<Map.Entry<T, byte[]>> keyValueStream = keyValueMap.entrySet().stream();
        keyValueStream = parallelEncode ? keyValueStream.parallel() : keyValueStream;
        keyValueStream.forEach(entry -> {
            byte[] keyByte = ObjectUtils.objectToByteArray(entry.getKey());
            int binIndex = binHash.getInteger(keyByte, binNum);
            keyValueMaps.get(binIndex).put(entry.getKey(), entry.getValue());
        });
        // encode
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        byte[][][] naiveStorage = binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .toArray(byte[][][]::new);
        // rearrange storage
        byte[][] sparseStorage = new byte[binNum * binM][byteL];
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            // copy sparse positions
            System.arraycopy(naiveStorage[binIndex], 0, sparseStorage, binLm * binIndex, binLm);
            // copy dense positions
            System.arraycopy(naiveStorage[binIndex], binLm, sparseStorage, binLm * binNum + binRm * binIndex, binRm);
        }
        return sparseStorage;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binSparsePositions = bins.get(binIndex).sparsePositions(key);
        boolean[] binDensePositions = bins.get(binIndex).binaryDensePositions(key);
        byte[] value = new byte[byteL];
        for (int binSparsePosition : binSparsePositions) {
            BytesUtils.xori(value, storage[binLm * binIndex + binSparsePosition]);
        }
        for (int binDensePosition = 0; binDensePosition < binRm; binDensePosition++) {
            if (binDensePositions[binDensePosition]) {
                BytesUtils.xori(value, storage[binLm * binNum + binRm * binIndex + binDensePosition]);
            }
        }
        return value;
    }
}

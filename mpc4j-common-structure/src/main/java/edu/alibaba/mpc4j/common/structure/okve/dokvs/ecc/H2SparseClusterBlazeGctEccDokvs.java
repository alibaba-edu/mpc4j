package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H2ClusterBlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.bouncycastle.math.ec.ECPoint;

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
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H2SparseClusterBlazeGctEccDokvs<T> extends AbstractH2ClusterBlazeGctEccDokvs<T> implements SparseEccDokvs<T> {

    H2SparseClusterBlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H2SparseClusterBlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
        super(envType, ecc, n, keys, secureRandom);
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
    public EccDokvsType getType() {
        return EccDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT;
    }

    @Override
    public ECPoint[] encode(Map<T, ECPoint> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        // create and split bins
        ArrayList<Map<T, ECPoint>> keyValueMaps = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ConcurrentHashMap<T, ECPoint>(binN))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<Entry<T, ECPoint>> keyValueStream = keyValueMap.entrySet().stream();
        keyValueStream = parallelEncode ? keyValueStream.parallel() : keyValueStream;
        keyValueStream.forEach(entry -> {
            byte[] keyByte = ObjectUtils.objectToByteArray(entry.getKey());
            int binIndex = binHash.getInteger(keyByte, binNum);
            keyValueMaps.get(binIndex).put(entry.getKey(), entry.getValue());
        });
        // encode
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        ECPoint[][] naiveStorage = binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .toArray(ECPoint[][]::new);
        // rearrange storage
        ECPoint[] sparseStorage = new ECPoint[binNum * binM];
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            // copy sparse positions
            System.arraycopy(naiveStorage[binIndex], 0, sparseStorage, binLm * binIndex, binLm);
            // copy dense positions
            System.arraycopy(naiveStorage[binIndex], binLm, sparseStorage, binLm * binNum + binRm * binIndex, binRm);
        }
        return sparseStorage;
    }

    @Override
    public ECPoint decode(ECPoint[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] binSparsePositions = bins.get(binIndex).sparsePositions(key);
        boolean[] binDensePositions = bins.get(binIndex).binaryDensePositions(key);
        ECPoint value = ecc.getInfinity();
        for (int binSparsePosition : binSparsePositions) {
            value = ecc.add(value, storage[binLm * binIndex + binSparsePosition]);
        }
        for (int binDensePosition = 0; binDensePosition < binRm; binDensePosition++) {
            if (binDensePositions[binDensePosition]) {
                value = ecc.add(value, storage[binLm * binNum + binRm * binIndex + binDensePosition]);
            }
        }
        return value;
    }
}

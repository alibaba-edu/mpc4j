package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

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
 * Clustering blazing fast DOKVS using garbled cuckoo table with 2 hash functions. The construction is from the paper:
 * <p>
 * Raghuraman, Srinivasan, and Peter Rindal. Blazing fast PSI from improved OKVS and subfield VOLE. ACM CCS 2022,
 * pp. 2505-2517.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/8/3
 */
class H2NaiveClusterBlazeGctGf2eDokvs<T> extends AbstractH2ClusterBlazeGctGf2eDokvs<T> {

    H2NaiveClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H2NaiveClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
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
    public Gf2eDokvsType getType() {
        return Gf2eDokvsType.H2_NAIVE_CLUSTER_BLAZE_GCT;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        // create and split bins
        ArrayList<Map<T, byte[]>> keyValueMaps = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new ConcurrentHashMap<T, byte[]>(binN))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<Entry<T, byte[]>> keyValueStream = keyValueMap.entrySet().stream();
        keyValueStream = parallelEncode ? keyValueStream.parallel() : keyValueStream;
        keyValueStream.forEach(entry -> {
            byte[] keyByte = ObjectUtils.objectToByteArray(entry.getKey());
            int binIndex = binHash.getInteger(keyByte, binNum);
            keyValueMaps.get(binIndex).put(entry.getKey(), entry.getValue());
        });
        // encode
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        return binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] positions = Arrays.stream(bins.get(binIndex).positions(key))
            .map(binPosition -> binPosition + binIndex * binM)
            .toArray();
        return BytesUtils.innerProduct(storage, byteL, positions);
    }
}

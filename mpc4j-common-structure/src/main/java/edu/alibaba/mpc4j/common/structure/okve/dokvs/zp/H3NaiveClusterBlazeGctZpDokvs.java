package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

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
 * Clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class H3NaiveClusterBlazeGctZpDokvs<T> extends AbstractH3ClusterBlazeGctZpDokvs<T> {

    H3NaiveClusterBlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys) {
        this(envType, p, n, keys, new SecureRandom());
    }

    H3NaiveClusterBlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
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
    public ZpDokvsType getType() {
        return ZpDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
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
        return binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .flatMap(Arrays::stream)
            .toArray(BigInteger[]::new);
    }

    @Override
    public BigInteger decode(BigInteger[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] positions = Arrays.stream(bins.get(binIndex).positions(key))
            .map(binPosition -> binPosition + binIndex * binM)
            .toArray();
        return zp.innerProduct(storage, positions);
    }
}

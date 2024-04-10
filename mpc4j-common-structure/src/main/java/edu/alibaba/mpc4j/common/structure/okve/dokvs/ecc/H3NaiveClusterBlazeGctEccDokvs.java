package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

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
 * Clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/7
 */
class H3NaiveClusterBlazeGctEccDokvs<T> extends AbstractH3ClusterBlazeGctEccDokvs<T> {

    H3NaiveClusterBlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys) {
        this(envType, ecc, n, keys, new SecureRandom());
    }

    H3NaiveClusterBlazeGctEccDokvs(EnvType envType, Ecc ecc, int n, byte[][] keys, SecureRandom secureRandom) {
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
    public EccDokvsType getType() {
        return EccDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
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
        return binIndexIntStream
            .mapToObj(binIndex -> bins.get(binIndex).encode(keyValueMaps.get(binIndex), doublyEncode))
            .flatMap(Arrays::stream)
            .toArray(ECPoint[]::new);
    }

    @Override
    public ECPoint decode(ECPoint[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int binIndex = binHash.getInteger(keyBytes, binNum);
        int[] positions = Arrays.stream(bins.get(binIndex).positions(key))
            .map(binPosition -> binPosition + binIndex * binM)
            .toArray();
        return ecc.innerProduct(storage, positions);
    }
}

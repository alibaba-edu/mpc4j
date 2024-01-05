package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2k.Gf2kDokvsFactory.Gf2kDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
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
 * field clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
class H3ClusterFieldBlazeGctGf2kDokvs<T> extends AbstractGf2kDokvs<T> implements Gf2kDokvs<T> {
    /**
     * number of hash keys, one more key for bin
     */
    static final int HASH_KEY_NUM = H3FieldBlazeGctGf2kDokvs.HASH_KEY_NUM + 1;
    /**
     * expected bin size, i.e., m^* = 2^14
     */
    private static final int EXPECT_BIN_SIZE = 1 << 14;

    /**
     * Gets m.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(int n) {
        MathPreconditions.checkPositive("n", n);
        int binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        int binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        int binLm = H3FieldBlazeGctGf2kDokvs.getLm(binN);
        int binRm = H3FieldBlazeGctGf2kDokvs.getRm(binN);
        int binM = binLm + binRm;
        return binNum * binM;
    }

    /**
     * number of bins
     */
    private final int binNum;
    /**
     * number of key-value pairs in each bin
     */
    private final int binN;
    /**
     * left m in each bin
     */
    private final int binLm;
    /**
     * right m in each bin
     */
    private final int binRm;
    /**
     * m for each bin
     */
    private final int binM;
    /**
     * bin hash
     */
    private final Prf binHash;
    /**
     * bins
     */
    private final ArrayList<H3FieldBlazeGctGf2kDokvs<T>> bins;

    H3ClusterFieldBlazeGctGf2kDokvs(EnvType envType, int n, byte[][] keys) {
        this(envType, n, keys, new SecureRandom());
    }

    H3ClusterFieldBlazeGctGf2kDokvs(EnvType envType, int n, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, getM(n), secureRandom);
        // calculate bin_num and bin_size
        binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        binLm = H3FieldBlazeGctGf2kDokvs.getLm(binN);
        binRm = H3FieldBlazeGctGf2kDokvs.getRm(binN);
        binM = binLm + binRm;
        // clone keys
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, HASH_KEY_NUM);
        // init bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(keys[0]);
        byte[][] cloneKeys = new byte[HASH_KEY_NUM - 1][];
        for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
            cloneKeys[keyIndex] = BytesUtils.clone(keys[keyIndex + 1]);
        }
        // create bins
        Kdf kdf = KdfFactory.createInstance(envType);
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
                    cloneKeys[keyIndex] = kdf.deriveKey(cloneKeys[keyIndex]);
                }
                return new H3FieldBlazeGctGf2kDokvs<T>(envType, binN, cloneKeys, secureRandom);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Gf2kDokvsType getType() {
        return Gf2kDokvsType.H3_CLUSTER_FIELD_BLAZE_GCT;
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
        int[] binSparsePositions = bins.get(binIndex).sparsePositions(key);
        byte[][] binDenseFields = bins.get(binIndex).denseFields(key);
        byte[] value = gf2k.createZero();
        // sparse part
        for (int binSparsePosition : binSparsePositions) {
            gf2k.addi(value, storage[binIndex * binM + binSparsePosition]);
        }
        // dense part
        for (int rmIndex = 0; rmIndex < binRm; rmIndex++) {
            gf2k.addi(value, gf2k.mul(storage[binIndex * binM + binLm + rmIndex], binDenseFields[rmIndex]));
        }
        return value;
    }
}

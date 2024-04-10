package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Mega-Bin DOKVS. The detailed construction comes from the following paper:
 * <p>
 * Pinkas B, Schneider T, Tkachenko O, et al. Efficient circuit-based PSI with linear communication. EUROCRYPT 2019.
 * Springer, Cham, pp. 122-153.
 * </p>
 * The brief introduction comes from Section 4 of the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021. Springer,
 * Cham, 2021: 591-617.
 * </p>
 * Call a mapping “y || i → s_{h_i(y)} ⊕ PRF(k_{h_i(y)}, y || i)” a hint. Bob must convey 3n such hints to Alice in
 * the protocol. One way to do this is to make n' = n / log(n) so-called mega-bins and assign each hint into a mega-bin
 * using a hash function - i.e., assign the hint for y || i to the mega-bin indexed H(y || i) for a public random
 * function H: {0, 1}^* → [n']. With these parameters, all mega-bins hold fewer than O(log n) items, with overwhelming
 * probability. Bob adds dummy hints to each mega-bin so that all mega-bins contain the worst-case O(log n) number of
 * hints (since the number of “real” hints per mega-bin leaks information about his input set). In each mega-bin, Bob
 * interpolates a polynomial over the hints in that bin, and sends all the polynomials to Alice. For each x || i held
 * by Alice, she can find the corresponding hint (if it exists) in the polynomial for the corresponding mega-bin.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
public class MegaBinGf2eDokvs<T> implements Gf2eDokvs<T> {
    /**
     * Gets the number of bins.
     *
     * @param n the number of key-value pairs.
     * @return the number of bins.
     */
    private static int getBinNum(int n) {
        MathPreconditions.checkPositive("n", n);
        if (n == 1) {
            // if n = 1, log(1) = 0 so that we may have Integer.MAX_VALUE bins.
            return 1;
        } else {
            // there are at least 1 bin.
            return Math.max(1, (int)Math.ceil(n / Math.log(n)));
        }
    }
    /**
     * Gets the bin size.
     *
     * @param n the number of key-value pairs.
     * @return the bin size.
     */
    private static int getBinSize(int n) {
        int b = getBinNum(n);
        // put n elements into b bins, with the constraint that there are at least 2 elements in each bin.
        return Math.max(2, MaxBinSizeUtils.expectMaxBinSize(n, b));
    }

    /**
     * Gets m.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(EnvType envType, int n) {
        int binNum = getBinNum(n);
        int binSize = getBinSize(n);
        return binNum * Gf2ePolyFactory.getCoefficientNum(envType, binSize);
    }

    /**
     * number of hash keys
     */
    static int HASH_KEY_NUM = 2;

    /**
     * the polynomial interpolation interface.
     */
    private final Gf2ePoly gf2ePoly;
    /**
     * the number of interpolated points
     */
    private final int n;
    /**
     * the key / value bit length, which must satisfies l % Byte.SIZE == 0
     */
    private final int l;
    /**
     * the key / value byte length
     */
    private final int byteL;
    /**
     * bin num: nlog(n)
     */
    private final int binNum;
    /**
     * bin size
     */
    private final int binSize;
    /**
     * m
     */
    private final int m;
    /**
     * hash used to compute the bin index.
     */
    private final Prf binHash;
    /**
     * hm: {0, 1}^* -> {0, 1}^l.
     */
    private final Prf hm;
    /**
     * parallel encode
     */
    private boolean parallelEncode;

    public MegaBinGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        // set the bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(keys[0]);
        // here we only need to require l > 0
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // set bin num and bin size.
        binNum = getBinNum(n);
        binSize = getBinSize(n);
        // set polynomial service
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
        // there are b bins, each bin contains O(log(n)) elements
        m = binNum * gf2ePoly.coefficientNum(binSize);
        // set mapping hash
        hm = PrfFactory.createInstance(envType, byteL);
        hm.setKey(keys[1]);
        parallelEncode = false;
    }

    @Override
    public Gf2eDokvsType getType() {
        return Gf2eDokvsType.MEGA_BIN;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        this.parallelEncode = parallelEncode;
    }

    @Override
    public boolean getParallelEncode() {
        return parallelEncode;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap, boolean doublyEncode) throws ArithmeticException {
        MathPreconditions.checkLessOrEqual("key-value size", keyValueMap.size(), n);
        Stream<Map.Entry<T, byte[]>> entryStream = keyValueMap.entrySet().stream();
        entryStream = parallelEncode ? entryStream.parallel() : entryStream;
        Map<ByteBuffer, byte[]> hashKeyValueMap = entryStream
            .collect(Collectors.toMap(
                entry -> {
                    T key = entry.getKey();
                    byte[] mapKey = hm.getBytes(ObjectUtils.objectToByteArray(key));
                    BytesUtils.reduceByteArray(mapKey, l);
                    return ByteBuffer.wrap(mapKey);
                },
                Map.Entry::getValue
            ));
        // construct the bin
        ArrayList<Map<ByteBuffer, byte[]>> bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashMap<ByteBuffer, byte[]>(binSize))
            .collect(Collectors.toCollection(ArrayList::new));
        // place each element into the bin
        for (Map.Entry<ByteBuffer, byte[]> entrySet : hashKeyValueMap.entrySet()) {
            int binIndex = binHash.getInteger(entrySet.getKey().array(), binNum);
            ByteBuffer x = entrySet.getKey();
            assert BytesUtils.isFixedReduceByteArray(x.array(), byteL, l);
            byte[] y = entrySet.getValue();
            assert BytesUtils.isFixedReduceByteArray(y, byteL, l);
            bins.get(binIndex).put(x, y);
        }
        // verify each bin
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            if (bins.get(binIndex).size() > binSize) {
                throw new ArithmeticException(String.format("bin[%s] exceeds max bin size", binIndex));
            }
        }
        // polynomial interpolation for each bin
        IntStream binIndexIntStream = IntStream.range(0, binNum);
        binIndexIntStream = parallelEncode ? binIndexIntStream.parallel() : binIndexIntStream;
        return binIndexIntStream
            .mapToObj(binIndex -> {
                Map<ByteBuffer, byte[]> bin = bins.get(binIndex);
                byte[][] xArray = bin.keySet().stream().map(ByteBuffer::array).toArray(byte[][]::new);
                byte[][] yArray = bin.keySet().stream().map(bin::get).toArray(byte[][]::new);
                return gf2ePoly.interpolate(binSize, xArray, yArray);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, m);
        byte[] hashKey = hm.getBytes(ObjectUtils.objectToByteArray(key));
        BytesUtils.reduceByteArray(hashKey, l);
        // compute the bin index
        int binIndex = binHash.getInteger(hashKey, binNum);
        // get the corresponding polynomial coefficients
        byte[][] coefficients = new byte[binSize][];
        System.arraycopy(storage, binIndex * binSize, coefficients, 0, binSize);

        return gf2ePoly.evaluate(coefficients, hashKey);
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getM() {
        return m;
    }
}

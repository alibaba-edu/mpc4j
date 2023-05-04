package edu.alibaba.mpc4j.common.tool.okve.basic;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mega-Bin basic OKVS. The detailed construction comes from the following paper:
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
 * @date 2023/3/27
 */
public class MegaBinBasicOkvs implements BasicOkvs {
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
     * hash used to compute the bin index.
     */
    private final Prf binHash;
    /**
     * parallel encode
     */
    private boolean parallelEncode;

    public MegaBinBasicOkvs(EnvType envType, int n, int l, byte[] key) {
        assert n > 0 : "n must be greater than 0: " + n;
        this.n = n;
        // set the bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(key);
        // l >= σ (40 bits)
        int minL = LongUtils.ceilLog2(n) + CommonConstants.STATS_BIT_LENGTH;
        assert l >= minL : "l must be greater than or equal to " + minL + ": " + l;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // set bin num and bin size.
        binNum = getBinNum(n);
        binSize = getBinSize(n);
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
        parallelEncode = false;
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
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n
            : "# of key-value pairs must be less than or equal to " + n + ": " + keyValueMap.size();
        // construct the bin
        ArrayList<Map<ByteBuffer, byte[]>> bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashMap<ByteBuffer, byte[]>(binSize))
            .collect(Collectors.toCollection(ArrayList::new));
        // place each element into the bin
        for (Map.Entry<ByteBuffer, byte[]> entrySet : keyValueMap.entrySet()) {
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
    public byte[] decode(byte[][] storage, ByteBuffer key) {
        // We do not need to verify byte length for each storage, which runs in O(n). We only verify storage.length
        assert storage.length == getM();
        // compute the bin index
        int binIndex = binHash.getInteger(key.array(), binNum);
        // get the corresponding polynomial coefficients
        byte[][] coefficients = new byte[binSize][];
        System.arraycopy(storage, binIndex * binSize, coefficients, 0, binSize);

        return gf2ePoly.evaluate(coefficients, key.array());
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
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getM() {
        // there are b bins, each bin contains O(log(n)) elements
        return binNum * gf2ePoly.coefficientNum(binSize);
    }

    @Override
    public BasicOkvsFactory.BasicOkvsType getType() {
        return BasicOkvsFactory.BasicOkvsType.MEGA_BIN;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }

    /**
     * Gets the number of bins.
     *
     * @param n the number of key-value pairs.
     * @return the number of bins.
     */
    static int getBinNum(int n) {
        assert n > 0 : "n must be greater than 0: " + n;
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
    static int getBinSize(int n) {
        int b = getBinNum(n);
        // put n elements into b bins, with the constraint that there are at least 2 elements in each bin.
        return Math.max(2, MaxBinSizeUtils.expectMaxBinSize(n, b));
    }
}

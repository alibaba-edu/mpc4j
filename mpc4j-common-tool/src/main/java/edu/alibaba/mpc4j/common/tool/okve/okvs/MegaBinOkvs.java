package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mega-Bin不经意键值对存储器（OKVS）。方案构造来自于论文：
 * Pinkas B, Schneider T, Tkachenko O, et al. Efficient circuit-based PSI with linear communication. EUROCRYPT 2019.
 * Springer, Cham, pp. 122-153.
 *
 * 方案构造的解释来自于下述论文第4节的上方：
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021. Springer,
 * Cham, 2021: 591-617.
 *
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
 * @date 2021/09/13
 */
class MegaBinOkvs implements Okvs<ByteBuffer> {
    /**
     * 多项式插值服务
     */
    private final Gf2ePoly gf2ePoly;
    /**
     * 插值数量
     */
    private final int n;
    /**
     * 插值对的比特长度，要求是Byte.SIZE的整数倍
     */
    private final int l;
    /**
     * 桶数量，等于n log(n)
     */
    private final int binNum;
    /**
     * 桶大小，等于简单哈希插入的桶大小
     */
    private final int binSize;
    /**
     * 分桶哈希
     */
    private final Prf binHash;
    /**
     * 是否并发编码
     */
    private boolean parallelEncode;

    MegaBinOkvs(EnvType envType, int n, int l, byte[] key) {
        // Mega-Bin要求至少编码2个元素
        assert n > 1;
        this.n = n;
        // 设置分桶哈希
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(key);
        // 要求l > 统计安全常数，且l可以被Byte.SIZE整除
        assert l >= CommonConstants.STATS_BIT_LENGTH && l % Byte.SIZE == 0;
        this.l = l;
        // 设置桶数量和桶深度
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
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n;
        // 构造分桶
        ArrayList<Map<ByteBuffer, byte[]>> bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashMap<ByteBuffer, byte[]>(binSize))
            .collect(Collectors.toCollection(ArrayList::new));
        // 将各个元素放置在桶中
        for (Map.Entry<ByteBuffer, byte[]> entrySet : keyValueMap.entrySet()) {
            int binIndex = binHash.getInteger(entrySet.getKey().array(), binNum);
            bins.get(binIndex).put(entrySet.getKey(), entrySet.getValue());
        }
        // 验证各个桶的大小
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            if (bins.get(binIndex).size() > binSize) {
                throw new ArithmeticException(String.format("bin[%s] exceeds max bin size", binIndex));
            }
        }
        // 为各个桶插值
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
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        // 计算桶序号
        int binIndex = binHash.getInteger(key.array(), binNum);
        byte[][] coefficients = new byte[binSize][];
        System.arraycopy(storage, binIndex * binSize, coefficients, 0, binSize);

        return gf2ePoly.evaluate(coefficients, key.array());
    }

    @Override
    public int getN() {
        return gf2ePoly.coefficientNum(n);
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getM() {
        // 一共b个桶，每个桶O(log(n))个元素
        return binNum * binSize;
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.MEGA_BIN;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }

    static int getBinNum(int n) {
        assert n > 1;
        // 至少要分一个桶
        return Math.max(1, (int)Math.ceil(n / Math.log(n)));
    }

    static int getBinSize(int n) {
        int b = getBinNum(n);
        // 将n个元素放入b个桶中，每个桶元素的最大数量，但每个桶至少有2个元素
        return Math.max(2, MaxBinSizeUtils.expectMaxBinSize(n, b));
    }
}

package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 解析哈希桶，可以用更小的数据量表示哈希桶中插入的元素。然而，由于插入和解析元素时需要使用代数操作，因此只支持插入BigInteger类型数据。
 *
 * 基本原理来自于下述论文第6.1节：
 * Suppose we are considering PSI on strings of length σ bits. Let h be a random function with output range {0, 1}^d,
 * where the number of bins is 2^d. To assign an item x to a bin, we write x = x_L || x_R, with |x_L| = d. We assign
 * this item to bin h(x_R) ⊕ x_L, and store it in that bin with x_R as its representation.
 *
 * The idea can be extended as follows, when the number m of bins is not a power of two (here h is taken to be a
 * function with range [m]):
 * - phase_{h, m}(x) = (h(⌊x / m⌋) + x mod m, ⌊x / m⌋)
 * - phase_{h, m}^{-1}(b, z) = zm + [h(z) + b mod m]
 *
 * Rindal, Peter, and Mike Rosulek. Malicious-secure private set intersection via dual execution. CCS 2017, pp.
 * 1229-1242. 2017.
 *
 * @author Weiran Liu
 * @date 2021/12/22
 */
public class PhaseHashBin implements HashBin<BigInteger> {
    /**
     * 带密钥哈希函数
     */
    private final Prf hash;
    /**
     * 哈希桶个数
     */
    private final int binNum;
    /**
     * 哈希桶个数的大整数表示
     */
    private final BigInteger bigIntBinNum;
    /**
     * 期望插入的元素总个数
     */
    private final int maxItemSize;
    /**
     * 每个哈希桶的最大元素个数
     */
    private final int maxBinSize;
    /**
     * 桶中元素的数量
     */
    private int itemSize;
    /**
     * 哈希桶是否已经插入了元素
     */
    private boolean insertedItems;
    /**
     * 填充元素数量
     */
    private int paddingItemSize;
    /**
     * 哈希桶是否填充了虚拟元素
     */
    private boolean insertedPaddingItems;
    /**
     * 哈希桶
     */
    private final ArrayList<ArrayList<HashBinEntry<BigInteger>>> bins;
    /**
     * 集合桶，用于快速判断桶中是否包含指定的元素
     */
    private final ArrayList<Set<HashBinEntry<BigInteger>>> setBins;

    /**
     * 初始化Phase哈希桶。
     *
     * @param envType 环境类型。
     * @param binNum 哈希桶数量。
     * @param maxItemSize 元素总个数。
     * @param key 哈希密钥。
     */
    public PhaseHashBin(EnvType envType, int binNum, int maxItemSize, byte[] key) {
        this(envType, binNum, MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum), maxItemSize, key);
    }

    /**
     * 初始化Phase哈希桶。
     *
     * @param envType 环境类型。
     * @param binNum 哈希桶数量。
     * @param maxBinSize 哈希桶最大元素个数。
     * @param maxItemSize 元素总个数。
     * @param key 哈希密钥。
     */
    public PhaseHashBin(EnvType envType, int binNum, int maxBinSize, int maxItemSize, byte[] key) {
        assert binNum > 0;
        assert maxBinSize > 0;
        // 哈希数量乘以最大元素数量 <= 桶数量 * 每个桶的最大数
        assert maxItemSize <= binNum * maxBinSize;
        this.binNum = binNum;
        bigIntBinNum = BigInteger.valueOf(binNum);
        this.maxBinSize = maxBinSize;
        this.maxItemSize = maxItemSize;
        // 初始化哈希函数
        hash = PrfFactory.createInstance(envType, Integer.BYTES);
        hash.setKey(key);
        // 初始化哈希桶
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                ArrayList<HashBinEntry<BigInteger>> bin = new ArrayList<>(maxBinSize);
                bin.ensureCapacity(maxBinSize);
                return bin;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        // 初始化集合哈希桶
        setBins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashSet<HashBinEntry<BigInteger>>(maxBinSize))
            .collect(Collectors.toCollection(ArrayList::new));
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
    }

    @Override
    public int getHashNum() {
        return 1;
    }

    @Override
    public byte[][] getHashKeys() {
        byte[][] hashKeys = new byte[1][];
        hashKeys[0] = hash.getKey();
        return hashKeys;
    }

    @Override
    public int maxItemSize() {
        return maxItemSize;
    }

    @Override
    public int binNum() {
        return binNum;
    }

    @Override
    public int maxBinSize() {
        return maxBinSize;
    }

    @Override
    public void insertItems(Collection<BigInteger> items) {
        assert (!insertedItems && !insertedPaddingItems);
        // 一次插入的元素数量小于等于预先设定好的数量
        assert items.size() <= maxItemSize;
        itemSize = 0;
        for (BigInteger item : items) {
            // Phase元素，放入到适当的桶中
            int binIndex = phaseIndex(item);
            BigInteger binItem = phaseItem(item);
            HashBinEntry<BigInteger> binHashEntry = HashBinEntry.fromRealItem(0, binItem);
            ArrayList<HashBinEntry<BigInteger>> bin = bins.get(binIndex);
            Set<HashBinEntry<BigInteger>> setBin = setBins.get(binIndex);
            if (setBin.add(binHashEntry)) {
                bin.add(binHashEntry);
                itemSize++;
            } else {
                // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
                clear();
                throw new IllegalArgumentException("Inserted items contain duplicate item: " + item);
            }
        }
        assert itemSize == items.size();
        // 计算各个桶的元素数量，看是否超过了预估的最大值
        IntStream.range(0, binNum).forEach(binIndex -> {
            int binSize = bins.get(binIndex).size();
            if (binSize > maxBinSize) {
                throw new ArithmeticException(
                    String.format("bin[%s] contains %s items, exceeding MaxBinSize = %s", binIndex, binSize, maxBinSize)
                );
            }
        });
        insertedItems = true;
    }

    /**
     * 计算元素需要放置到的哈希桶索引，计算逻辑为h(⌊x / m⌋) + x mod m，其中m是桶的个数。
     *
     * @param item 元素。
     * @return 元素放置到的哈希桶索引。
     */
    public int phaseIndex(BigInteger item) {
        assert BigIntegerUtils.greaterOrEqual(item, BigInteger.ZERO);
        // 将输入的数据转换为正数
        return item
            .add(BigInteger.valueOf(hash.getInteger(item.divide(bigIntBinNum).toByteArray(), binNum)))
            .remainder(bigIntBinNum).intValue();
    }

    /**
     * 计算元素放置在哈希桶中时需要放置的编码元素，即⌊x / m⌋。
     *
     * @param item 元素。
     * @return 哈希桶中放置的编码元素。
     */
    public BigInteger phaseItem(BigInteger item) {
        assert BigIntegerUtils.greaterOrEqual(item, BigInteger.ZERO);
        return item.divide(bigIntBinNum);
    }

    /**
     * 根据桶的索引值和桶插入的编码元素，恢复出原始元素。
     *
     * @param phaseIndex 元素放置到的哈希桶索引。
     * @param phaseItem  哈希桶中放置的编码元素。
     * @return 原始元素。
     */
    public BigInteger dephaseItem(int phaseIndex, BigInteger phaseItem) {
        assert phaseIndex >= 0 && phaseIndex < binNum;
        int mod = phaseIndex - hash.getInteger(phaseItem.toByteArray(), binNum);
        mod = mod < 0 ? mod + binNum : mod;
        return BigInteger.valueOf(mod).remainder(bigIntBinNum).add(phaseItem.multiply(bigIntBinNum));
    }

    @Override
    public boolean insertedItems() {
        return insertedItems;
    }

    @Override
    public int itemSize() {
        return itemSize;
    }

    @Override
    public boolean contains(BigInteger item) {
        int binIndex = phaseIndex(item);
        BigInteger binItem = phaseItem(item);
        HashBinEntry<BigInteger> binHashEntry = HashBinEntry.fromRealItem(0, binItem);
        return setBins.get(binIndex).contains(binHashEntry);
    }

    @Override
    public HashBinEntry<BigInteger> get(BigInteger item) {
        if (contains(item)) {
            return HashBinEntry.fromRealItem(0, item);
        }
        // 不包含改定元素，返回null
        return null;
    }

    /**
     * 返回给定元素的Phase元素。
     *
     * @param item 给定的元素。
     * @return 返回给定元素的Phase元素，如果哈希桶不包含给定的元素，则返回null。
     */
    public HashBinEntry<BigInteger> getPhaseItem(BigInteger item) {
        if (contains(item)) {
            BigInteger binItem = phaseItem(item);
            return HashBinEntry.fromRealItem(0, binItem);
        }
        // 不包含给定元素，返回null
        return null;
    }

    @Override
    public List<HashBinEntry<BigInteger>> getBin(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex);
    }

    @Override
    public int binSize(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex).size();
    }

    /**
     * 插入空填充元素。
     *
     * @param emptyItem 空元素。
     */
    public void insertPaddingItems(BigInteger emptyItem) {
        assert (insertedItems && !insertedPaddingItems);
        // 桶中插入随机元素
        paddingItemSize = 0;
        IntStream.range(0, binNum).forEach(binIndex -> {
            Set<HashBinEntry<BigInteger>> setBin = setBins.get(binIndex);
            HashBinEntry<BigInteger> emptyHashBinEntry = HashBinEntry.fromEmptyItem(emptyItem);
            if (setBin.contains(emptyHashBinEntry)) {
                throw new IllegalArgumentException(String.format(
                    "bin[%s] already contains empty item %s", binIndex, emptyItem
                ));
            }
            // 在列表桶中插入空元素
            ArrayList<HashBinEntry<BigInteger>> bin = bins.get(binIndex);
            // 如果相应的桶没满，则一直填充到满
            while (bin.size() < maxBinSize) {
                bin.add(HashBinEntry.fromEmptyItem(emptyItem));
                paddingItemSize++;
            }
            // 对桶进行随机置换
            Collections.shuffle(bin);
        });
        insertedPaddingItems = true;
    }

    @Override
    public boolean insertedPaddingItems() {
        return insertedPaddingItems;
    }

   @Override
    public int paddingItemSize() {
        return paddingItemSize;
    }

    @Override
    public int size() {
        return itemSize + paddingItemSize;
    }

    @Override
    public void clear() {
        // 清空列表桶中的元素
        for (List<HashBinEntry<BigInteger>> bin : bins) {
            bin.clear();
        }
        // 清空哈希桶中的元素
        for (Set<HashBinEntry<BigInteger>> setBin : setBins) {
            setBin.clear();
        }
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
    }
}

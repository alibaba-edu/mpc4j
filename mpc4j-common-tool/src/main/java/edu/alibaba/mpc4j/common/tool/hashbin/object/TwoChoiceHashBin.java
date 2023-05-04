package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 选择哈希函数（Choice Hashing）的实现。
 * 选择哈希一共包含两个哈希函数h_1和h_2，每个哈希表包含m个桶B_1,...,B_m。将每一个输入元素x映射到桶B_{h_1(y)}或者桶B_{h_2(y)}中。
 *
 * 下述描述摘自论文的Section 4.2, Theorem 4.1和Section 4.3, Figure 7：
 * Let h_1, h_2 be two random functions. Suppose there are n items and m bins, where each item x can be placed
 * in h_1(x) or h_2(x).
 * Let L = ⌊n / m⌋, then with high probability, each bin contains no more than L items.
 * Setting n / m = λ = 40 leads to the most dummy items one would ever consider.
 *
 * Pinkas B, Rosulek M, Trieu N, et al. SpOT-Light: Lightweight Private Set Intersection from Sparse OT Extension.
 * CRYPTO 2019, Springer, 2019, pp. 401-431.
 *
 * @author Weiran Liu
 * @date 2021/12/23
 */
public class TwoChoiceHashBin<T> implements HashBin<T> {
    /**
     * 计算哈希桶数量。
     *
     * @param maxItemSize 插入的元素数量。
     * @return 哈希桶数量。
     */
    public static int expectedBinNum(int maxItemSize) {
        // Setting n / m = λ = 40 leads to the most dummy items one would ever consider.
        return (int)Math.ceil((double)maxItemSize / CommonConstants.STATS_BIT_LENGTH);
    }

    /**
     * 计算哈希桶中存储元素的最大数量。
     *
     * @param maxItemSize 插入的元素数量。
     * @return 哈希桶中存储元素的最大数量。
     */
    public static int expectedMaxBinSize(int maxItemSize) {
        int expectedBinNum = TwoChoiceHashBin.expectedBinNum(maxItemSize);
        // 这里需要多加一个元素，测试结果表明，当n = 2^16时，桶的数量会变为41
        return (int)Math.ceil((double)maxItemSize / expectedBinNum) + 1;
    }

    /**
     * 哈希桶个数
     */
    private final int binNum;
    /**
     * 期望插入的元素数量
     */
    private final int maxItemSize;
    /**
     * 每个桶的最大元素数量
     */
    private final int maxBinSize;
    /**
     * 带密钥哈希函数h0
     */
    private final Prf h0;
    /**
     * 带密钥哈希函数h1
     */
    private final Prf h1;
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
     * 用集合表示的桶
     */
    private final ArrayList<Set<HashBinEntry<T>>> bins;

    /**
     * 初始化选择哈希桶。
     *
     * @param envType     环境类型。
     * @param maxItemSize 插入元素数量。
     * @param h0Key 哈希函数h0的密钥。
     * @param h1Key 哈希函数h1的密钥。
     */
    public TwoChoiceHashBin(EnvType envType, int maxItemSize, byte[] h0Key, byte[] h1Key) {
        assert maxItemSize > 0;
        this.maxItemSize = maxItemSize;
        binNum = TwoChoiceHashBin.expectedBinNum(maxItemSize);
        maxBinSize = TwoChoiceHashBin.expectedMaxBinSize(maxItemSize);
        h0 = PrfFactory.createInstance(envType, Integer.BYTES);
        h0.setKey(h0Key);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(h1Key);
        // 初始化哈希桶
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashSet<HashBinEntry<T>>(maxBinSize))
            .collect(Collectors.toCollection(ArrayList::new));
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
    }

    @Override
    public int getHashNum() {
        return 2;
    }

    @Override
    public byte[][] getHashKeys() {
        byte[][] hashKeys = new byte[2][];
        hashKeys[0] = h0.getKey();
        hashKeys[1] = h1.getKey();
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
    public void insertItems(Collection<T> items) {
        assert (!insertedItems && !insertedPaddingItems);
        // 一次插入的元素数量小于等于预先设定好的数量
        assert items.size() <= maxItemSize;
        // 执行论文中的Algorithm 1: FindAssignment步骤
        itemSize = 0;
        for (T item : items) {
            //for x \in X, Assign item x to bin h_1(x)
            HashBinEntry<T> hash1BinItem = HashBinEntry.fromRealItem(0, item);
            int binIndex1 = h0.getInteger(hash1BinItem.getItemByteArray(), binNum);
            Set<HashBinEntry<T>> bin = bins.get(binIndex1);
            if (bin.add(hash1BinItem)) {
                itemSize++;
            } else {
                // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
                clear();
                throw new IllegalArgumentException("Inserted items contain duplicate item: " + item);
            }
        }
        for (T item : items) {
            // for x \in X, Assign item x to whichever of h_1(x), h_2(x) currently has fewest items
            HashBinEntry<T> hash0BinItem = HashBinEntry.fromRealItem(0, item);
            int binIndex0 = h0.getInteger(hash0BinItem.getItemByteArray(), binNum);
            int binIndex1 = h1.getInteger(hash0BinItem.getItemByteArray(), binNum);
            Set<HashBinEntry<T>> hash0Bin = bins.get(binIndex0);
            Set<HashBinEntry<T>> hash1Bin = bins.get(binIndex1);
            if (hash0Bin.size() > hash1Bin.size()) {
                // 如果h_0对应的桶元素数量比h_1对应的桶元素数量多，则把元素从h_0移动到h_1
                if (hash0Bin.remove(hash0BinItem)) {
                    itemSize--;
                } else {
                    // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
                    clear();
                    throw new IllegalStateException(
                        String.format("Cannot remove already inserted item %s in bin[%s]", item, binIndex0)
                    );
                }
                HashBinEntry<T> hash1BinItem = HashBinEntry.fromRealItem(1, item);
                if (hash1Bin.add(hash1BinItem)) {
                    itemSize++;
                } else {
                    // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
                    clear();
                    throw new IllegalArgumentException("Inserted items contain duplicate item: " + item);
                }
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

    @Override
    public boolean insertedItems() {
        return insertedItems;
    }

    @Override
    public int itemSize() {
        return itemSize;
    }

    @Override
    public boolean contains(T item) {
        HashBinEntry<T> hash0BinItem = HashBinEntry.fromRealItem(0, item);
        HashBinEntry<T> hash1BinItem = HashBinEntry.fromRealItem(1, item);
        int hash0BinIndex = h0.getInteger(hash0BinItem.getItemByteArray(), binNum);
        int hash1BinIndex = h1.getInteger(hash1BinItem.getItemByteArray(), binNum);
        Set<HashBinEntry<T>> hash0Bin = bins.get(hash0BinIndex);
        Set<HashBinEntry<T>> hash1Bin = bins.get(hash1BinIndex);
        return (hash0Bin.contains(hash0BinItem) || hash1Bin.contains(hash1BinItem));
    }

    @Override
    public HashBinEntry<T> get(T item) {
        HashBinEntry<T> hash0BinItem = HashBinEntry.fromRealItem(0, item);
        HashBinEntry<T> hash1BinItem = HashBinEntry.fromRealItem(1, item);
        int hash0BinIndex = h0.getInteger(hash0BinItem.getItemByteArray(), binNum);
        int hash1BinIndex = h1.getInteger(hash1BinItem.getItemByteArray(), binNum);
        Set<HashBinEntry<T>> hash0Bin = bins.get(hash0BinIndex);
        Set<HashBinEntry<T>> hash1Bin = bins.get(hash1BinIndex);
        if (hash0Bin.contains(hash0BinItem)) {
            return hash0BinItem;
        } else if (hash1Bin.contains(hash1BinItem)) {
            return hash1BinItem;
        } else {
            return null;
        }
    }

    @Override
    public Collection<HashBinEntry<T>> getBin(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex);
    }

    @Override
    public int binSize(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex).size();
    }

    public void insertPaddingItems(SecureRandom secureRandom) {
        assert (insertedItems && !insertedPaddingItems);
        // 开始插入虚拟元素
        paddingItemSize = 0;
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            Set<HashBinEntry<T>> bin = bins.get(binIndex);
            // 如果相应的桶没满，则一直填充到满
            while (bin.size() < maxBinSize) {
                if (bin.add(HashBinEntry.fromDummyItem(secureRandom))) {
                    paddingItemSize++;
                }
            }
        }
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
        for (Set<HashBinEntry<T>> bin : bins) {
            bin.clear();
        }
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
    }
}

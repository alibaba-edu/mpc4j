package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 随机填充元素的简单哈希桶（Simple Hashing）的实现。简单哈希桶中，每个哈希表包含b个桶B_1,...,B_b。将每一个输入元素e映射到桶B_{h(e)}中。
 * 这里只实现单一哈希函数的简单哈希桶，从而与PhaseHash对应。
 *
 * In the simplest hashing scheme, the binhash table consists of b bins B_1, ..., B_b. Hashing is done by mapping each
 * input element e to a bin B_{h(e)} using a binhash function h: {0, 1}^σ → [1, b] that was chosen uniformly at random
 * and independently of the input elements.
 * An element is always added to the bin to which it is mapped, regardless of whether other elements are already stored
 * in that bin.
 *
 * 实现代码参考下述论文的C/C++实现（https://github.com/osu-crypto/BaRK-OPRF）：
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection。 CCS 2016, ACM, pp. 818-829.
 *
 * @author Weiran Liu
 * @date 2021/12/22
 */
public class RandomPadHashBin<T> implements HashBin<T> {
    /**
     * 带密钥哈希函数
     */
    private final Prf[] hashes;
    /**
     * 哈希桶个数
     */
    private final int binNum;
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
     * 用集合表示的桶
     */
    private final ArrayList<Set<HashBinEntry<T>>> bins;

    /**
     * 初始化随机填充哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxItemSize 元素总个数。
     * @param keys         哈希密钥。
     */
    public RandomPadHashBin(EnvType envType, int binNum, int maxItemSize, byte[][] keys) {
        this(envType, binNum, MaxBinSizeUtils.expectMaxBinSize(keys.length * maxItemSize, binNum), maxItemSize, keys);
    }

    /**
     * 初始化随机填充哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxBinSize  哈希桶最大元素个数。
     * @param maxItemSize 元素总个数。
     * @param keys 哈希密钥。
     */
    public RandomPadHashBin(EnvType envType, int binNum, int maxBinSize, int maxItemSize, byte[][] keys) {
        assert keys.length > 0;
        assert binNum > 0;
        assert maxBinSize > 0;
        // 哈希数量乘以最大元素数量 <= 桶数量 * 每个桶的最大数
        assert keys.length * maxItemSize <= binNum * maxBinSize;
        this.binNum = binNum;
        this.maxBinSize = maxBinSize;
        this.maxItemSize = maxItemSize;
        // 初始化哈希函数
        hashes = Arrays.stream(keys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
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
        return hashes.length;
    }

    @Override
    public byte[][] getHashKeys() {
        return Arrays.stream(hashes)
            .map(Prf::getKey)
            .toArray(byte[][]::new);
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
        itemSize = 0;
        for (T item : items) {
            // 遍历所有的哈希函数，对插入的元素求哈希，并插入到对应的桶中
            for (int hashIndex = 0; hashIndex < hashes.length; hashIndex++) {
                HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
                int binIndex = hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum);
                // 桶添加元素
                Set<HashBinEntry<T>> setBin = bins.get(binIndex);
                if (setBin.add(hashBinEntry)) {
                    itemSize++;
                } else {
                    // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
                    clear();
                    throw new IllegalArgumentException("Inserted items contain duplicate item: " + item);
                }
            }
        }
        assert itemSize == items.size() * hashes.length;
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
        // 只要第0个哈希所对应的BinHashItem在简单哈希里面，就意味着简单哈希包含此元素
        HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(0, item);
        int binIndex = hashes[0].getInteger(hashBinEntry.getItemByteArray(), binNum);
        return bins.get(binIndex).contains(hashBinEntry);
    }

    @Override
    public HashBinEntry<T> get(T item) {
        if (contains(item)) {
            return HashBinEntry.fromRealItem(0, item);
        }
        // 不包含改定元素，返回null
        return null;
    }

    @Override
    public Set<HashBinEntry<T>> getBin(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex);
    }

    @Override
    public int binSize(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return bins.get(binIndex).size();
    }

    /**
     * 插入虚拟填充元素。
     *
     * @param secureRandom 随机状态。
     */
    public void insertPaddingItems(SecureRandom secureRandom) {
        assert (insertedItems && !insertedPaddingItems);
        // 桶中插入随机元素
        paddingItemSize = 0;
        for (int binIndex = 0; binIndex < binNum; binIndex++) {
            Set<HashBinEntry<T>> bin = bins.get(binIndex);
            // 如果相应的桶没满，则一直填充到满
            while (bin.size() < maxBinSize) {
                HashBinEntry<T> dummyItem = HashBinEntry.fromDummyItem(secureRandom);
                if (bin.add(dummyItem)) {
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

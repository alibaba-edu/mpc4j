package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * 标准布谷鸟哈希函数（Cuckoo Hashing）的实现。
 * <p>
 * Cuckoo hashing uses k binhash functions h_1, ..., h_k : {0, 1}^σ → [1, b] to prf m elements to b = εn bins.
 * </p>
 * <p>
 * The scheme avoids collisions by relocating elements when a collision is found using the following procedure:
 * An element e is inserted into a bin B_{h_1(e)}. Any prior contents o of B_{h_1}(e) are evicted to a new bin Bhi(o),
 * using h_i to determine the new bin location, where h_i(o) \neq h_1(e) for i \in [1...k].
 * The procedure is repeated until no more evictions are necessary, or until a threshold number of relocations has been
 * performed.
 * </p>
 * <p>
 * In the latter case, the last element is put in a special stash.
 * </p>
 * Cuckoo hashing的提出论文为：
 * <p>
 * Pagh R, Rodler F F. Cuckoo hashing[J]. Journal of Algorithms, 2004, 51(2): 122-144.
 * </p>
 * 实现代码参考下述论文对应的C/C++实现：
 * <p>
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection。 CCS 2016, ACM, pp. 818-829.
 * </p>
 * 链接为：
 * <p>
 * https://github.com/osu-crypto/BaRK-OPRF
 * </p>
 *
 * @author Weiran Liu
 * @date 2019/10/09
 */
class NaiveCuckooHashBin<T> implements CuckooHashBin<T> {
    /**
     * 返回朴素布谷鸟哈希的桶数量。
     *
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 插入的元素数量。
     * @return 桶数量。
     */
    static int getBinNum(CuckooHashBinType type, int maxItemSize) {
        return (int) Math.ceil(maxItemSize * getEpsilon(type));
    }

    /**
     * 返回朴素布谷鸟哈希的插入的元素数量。
     *
     * @param type   布谷鸟哈希类型。
     * @param binNum 桶数量。
     * @return 插入的元素数量。
     */
    static int getMaxItemSize(CuckooHashBinType type, int binNum) {
        return (int) Math.floor(binNum / getEpsilon(type));
    }

    /**
     * 返回朴素布谷鸟哈希的ε取值。
     * 参数来自于PSSZ15论文，Section 3.2.2: Cuckoo Hashing Parameter Analysis中的Adjusting the Number of Hash Function k。
     *
     * @param type 布谷鸟哈希类型。
     */
    private static double getEpsilon(CuckooHashBinType type) {
        switch (type) {
            case NAIVE_2_HASH:
                // k = 2时，ε = 2.4
                return 2.4;
            case NAIVE_3_HASH:
                // k = 3时，ε = 1.2
                return 1.2;
            case NAIVE_4_HASH:
                // k = 4时，ε = 1.07
                return 1.07;
            case NAIVE_5_HASH:
                // k = 5时，ε = 1.04
                return 1.04;
            default:
                throw new IllegalArgumentException("Invalid NativeCuckooHashBinType:" + type.name());
        }
    }

    /**
     * 返回朴素布谷鸟哈希的暂存区大小，参数是PSSZ15论文Section 3.2 Cuckoo Hash（论文第15页）给出的测试结果。
     * 当按照此种方式设置时，真正执行朴素布谷鸟哈希时，贮存区超过给定阈值的概率小于2^{-40}。
     *
     * @param maxItemSize 插入元素数量。
     * @return 贮存区大小。
     */
    static int getStashSize(int maxItemSize) {
        MathPreconditions.checkPositiveInRangeClosed(
            "maxItemSize", maxItemSize, CuckooHashBinFactory.MAX_ITEM_SIZE_UPPER_BOUND
        );
        if (maxItemSize > 1 << 20) {
            // 当2^20 < maxItemSize <= 2^24，stash size = 2
            return 2;
        } else if (maxItemSize > 1 << 16) {
            // 当2^16 < maxItemSize <= 2^20，stash size = 3
            return 3;
        } else if (maxItemSize > 1 << 12) {
            // 当2^12 < maxItemSize <= 2^16，stash size = 4
            return 4;
        } else if (maxItemSize > 1 << 8) {
            // 当2^8 < maxItemSize <= 2^12，stash size = 6
            return 6;
        } else {
            // 当0 < maxItemSize <= 2^8，stash size = 12
            return 12;
        }
    }

    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType type;
    /**
     * Cuckoo Hash所能存储的最大元素数量
     */
    private final int maxItemSize;
    /**
     * 哈希函数的总个数
     */
    private final int hashNum;
    /**
     * 暂存区大小
     */
    private final int stashSize;
    /**
     * 哈希桶数量
     */
    private final int binNum;
    /**
     * 使用到的伪随机函数，实际上是哈希函数的个数
     */
    private final Prf[] hashes;
    /**
     * 桶
     */
    private final HashBinEntry<T>[] bins;
    /**
     * 暂存区
     */
    private final ArrayList<HashBinEntry<T>> stash;
    /**
     * 哈希桶中存储的元素数量
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
     * 初始化朴素布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 最大元素数量。
     * @param keys        哈希密钥。
     */
    NaiveCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, byte[][] keys) {
        this(envType, type, maxItemSize, CuckooHashBinFactory.getBinNum(type, maxItemSize), keys);
    }

    /**
     * 初始化朴素布谷鸟哈希。
     *
     * @param envType     环境类型。
     * @param type        布谷鸟哈希类型。
     * @param maxItemSize 最大元素数量。
     * @param keys        哈希密钥。
     */
    NaiveCuckooHashBin(EnvType envType, CuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
        // 初始化布谷鸟哈希类型
        this.type = type;
        // 设置参数
        this.maxItemSize = maxItemSize;
        hashNum = keys.length;
        this.binNum = binNum;
        // 初始化带密钥哈希函数
        hashes = Arrays.stream(keys).map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        itemSize = 0;
        paddingItemSize = 0;
        // 初始化储藏区大小
        stashSize = CuckooHashBinFactory.getStashSize(type, maxItemSize);
        //noinspection unchecked
        bins = new HashBinEntry[binNum];
        // 初始化Cuckoo Hash的贮藏区
        stash = new ArrayList<>(stashSize);
        // 初始时未填充虚拟元素，暂时未插入元素
        insertedItems = false;
        insertedPaddingItems = false;
    }

    @Override
    public CuckooHashBinType getType() {
        return type;
    }

    @Override
    public int itemNumInBins() {
        return itemSize + paddingItemSize - stash.size();
    }

    @Override
    public int itemNumInStash() {
        return stash.size();
    }

    @Override
    public ArrayList<HashBinEntry<T>> getStash() {
        return stash;
    }

    @Override
    public int stashSize() {
        return stashSize;
    }

    @Override
    public void insertItems(Collection<T> items) {
        Preconditions.checkArgument(!insertedItems && !insertedPaddingItems);
        // 一次插入的元素数量要小于等于预先设定好的数量
        MathPreconditions.checkNonNegativeInRangeClosed("itemSize", items.size(), maxItemSize);
        for (T item : items) {
            insertItem(item);
        }
        insertedItems = true;
    }

    private void insertItem(T item) {
        if (contains(item)) {
            clear();
            throw new IllegalArgumentException("Inserted items contain duplicate item: " + item);
        } else {
            insertItem(item, 0, 0);
        }
    }

    private void insertItem(T item, int hashIndex, int totalTries) {
        if (totalTries > CuckooHashBinFactory.DEFAULT_MAX_TOTAL_TRIES) {
            if (stash.size() >= stashSize) {
                int currentItemSize = itemSize;
                clear();
                throw new ArithmeticException(
                    String.format("Failed to insert items after %s items, stash exceeding StashSize = %s",
                        currentItemSize, stashSize
                    )
                );
            }
            // 超过最大迭代次数，将此元素放置在储藏区，binhash index可以随便设置，这里设置为numOfHashes
            HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashNum, item);
            stash.add(hashBinEntry);
            itemSize++;
        } else {
            // 如果没有超过最大迭代次数，则继续迭代
            totalTries++;
            HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum);
            HashBinEntry<T> existHashBinEntry = bins[binIndex];
            if (existHashBinEntry == null) {
                // 如果binIndex对应的数据为空，则将当前的数据放置在binAddress中
                bins[binIndex] = hashBinEntry;
                itemSize++;
            } else {
                // 如果binIndex对应的数据不为空，则把这部分数据取出来，重新放置到另一个binAddress里面
                T evictItem = existHashBinEntry.getItem();
                int evictItemHashIndex = existHashBinEntry.getHashIndex();
                bins[binIndex] = hashBinEntry;
                insertItem(evictItem, ((evictItemHashIndex + 1) % hashNum), totalTries);
            }
        }
    }

    @Override
    public boolean insertedItems() {
        return insertedItems;
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
    public int itemSize() {
        return itemSize;
    }

    @Override
    public boolean contains(T item) {
        // 判断不同哈希函数对应的桶是否包含给定的元素
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum);
            if (bins[binIndex] == null) {
                continue;
            }
            if (bins[binIndex].equals(hashBinEntry)) {
                return true;
            }
        }
        // 判断储存区是否包含给定的元素，如果都不包含，则的确不包含此元素
        HashBinEntry<T> stashBinEntry = HashBinEntry.fromRealItem(hashNum, item);
        return stash.contains(stashBinEntry);
    }

    @Override
    public HashBinEntry<T> get(T item) {
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum);
            if (bins[binIndex] == null) {
                continue;
            }
            if (bins[binIndex].equals(hashBinEntry)) {
                return hashBinEntry;
            }
        }
        // 在stash中寻找元素
        for (HashBinEntry<T> binHashEntry : stash) {
            if (binHashEntry.getItem().equals(item)) {
                return binHashEntry;
            }
        }
        return null;
    }

    @Override
    public int binSize(int binIndex) {
        MathPreconditions.checkNonNegativeInRange("binIndex", binIndex, binNum);
        return bins[binIndex] == null ? 0 : 1;
    }

    @Override
    public HashBinEntry<T> getHashBinEntry(int binIndex) {
        MathPreconditions.checkNonNegativeInRange("binIndex", binIndex, binNum);
        return bins[binIndex];
    }

    @Override
    public int binNum() {
        return binNum;
    }

    @Override
    public void insertPaddingItems(SecureRandom secureRandom) {
        Preconditions.checkArgument(insertedItems && !insertedPaddingItems);
        // 在bin中添加虚拟元素
        paddingItemSize = 0;
        for (int binIndex = 0; binIndex < binNum(); binIndex++) {
            // 如果相应的桶为空，则随便添加一个元素
            if (bins[binIndex] == null) {
                bins[binIndex] = HashBinEntry.fromDummyItem(secureRandom);
                paddingItemSize++;
            }
        }
        // 在stash中添加dummy item
        for (int stashIndex = stash.size(); stashIndex < stashSize; stashIndex++) {
            stash.add(HashBinEntry.fromDummyItem(secureRandom));
            paddingItemSize++;
        }
        insertedPaddingItems = true;
    }

    @Override
    public void insertPaddingItems(T emptyItem) {
        Preconditions.checkArgument(insertedItems && !insertedPaddingItems);
        // 插入的空元素不能为已存在的元素
        Preconditions.checkArgument(!contains(emptyItem));
        // 在bin中添加虚拟元素
        paddingItemSize = 0;
        for (int binIndex = 0; binIndex < binNum(); binIndex++) {
            // 如果相应的桶为空，则随便添加一个元素
            if (bins[binIndex] == null) {
                bins[binIndex] = HashBinEntry.fromEmptyItem(emptyItem);
                paddingItemSize++;
            }
        }
        // 在stash中添加dummy item
        for (int stashIndex = stash.size(); stashIndex < stashSize; stashIndex++) {
            stash.add(HashBinEntry.fromEmptyItem(emptyItem));
            paddingItemSize++;
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
        Arrays.fill(bins, null);
        stash.clear();
        paddingItemSize = 0;
        itemSize = 0;
        insertedPaddingItems = false;
        insertedItems = false;
    }
}
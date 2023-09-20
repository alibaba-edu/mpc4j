package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;

/**
 * 无暂存区布谷鸟哈希桶抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class AbstractNoStashCuckooHashBin<T> implements NoStashCuckooHashBin<T> {
    /**
     * 布谷鸟哈希桶类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType type;
    /**
     * 布谷鸟哈希通所能存储的最大元素数量
     */
    private final int maxItemSize;
    /**
     * 哈希函数的总个数
     */
    private final int hashNum;
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

    AbstractNoStashCuckooHashBin(EnvType envType, CuckooHashBinFactory.CuckooHashBinType type, int maxItemSize, byte[][] keys) {
        this(envType, type, maxItemSize, CuckooHashBinFactory.getBinNum(type, maxItemSize), keys);
    }

    AbstractNoStashCuckooHashBin(EnvType envType, CuckooHashBinFactory.CuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
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
        //noinspection unchecked
        bins = new HashBinEntry[binNum];
        // 初始时未填充虚拟元素，暂时未插入元素
        insertedItems = false;
        insertedPaddingItems = false;
    }

    @Override
    public CuckooHashBinFactory.CuckooHashBinType getType() {
        return type;
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
            int currentItemSize = itemSize;
            clear();
            throw new ArithmeticException(
                String.format("Failed to insert items after %s items, no position to put by %s tries",
                    currentItemSize, CuckooHashBinFactory.DEFAULT_MAX_TOTAL_TRIES
                )
            );
        } else {
            // 如果没有超过最大迭代次数，则继续迭代
            totalTries++;
            HashBinEntry<T> binHashEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(binHashEntry.getItemByteArray(), binNum);
            HashBinEntry<T> existBinHashEntry = bins[binIndex];
            if (existBinHashEntry == null) {
                // 如果binIndex对应的数据为空，则将当前的数据放置在binIndex中
                bins[binIndex] = binHashEntry;
                itemSize++;
            } else {
                // 如果binAddress对应的数据不为空，则把这部分数据取出来，重新放置到另一个binAddress里面
                T evictItem = existBinHashEntry.getItem();
                int evictItemHashIndex = existBinHashEntry.getHashIndex();
                bins[binIndex] = binHashEntry;
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
            HashBinEntry<T> binHashEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(binHashEntry.getItemByteArray(), binNum);
            if (bins[binIndex] == null) {
                continue;
            }
            if (bins[binIndex].equals(binHashEntry)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HashBinEntry<T> get(T item) {
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            HashBinEntry<T> binHashEntry = HashBinEntry.fromRealItem(hashIndex, item);
            int binIndex = hashes[hashIndex].getInteger(binHashEntry.getItemByteArray(), binNum);
            if (bins[binIndex] == null) {
                continue;
            }
            if (bins[binIndex].equals(binHashEntry)) {
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
        for (int binIndex = 0; binIndex < binNum(); binIndex++) {
            // 如果相应的桶为空，则随便添加一个元素
            if (bins[binIndex] == null) {
                bins[binIndex] = HashBinEntry.fromDummyItem(secureRandom);
                paddingItemSize++;
            }
        }
        insertedPaddingItems = true;
    }

    @Override
    public void insertPaddingItems(T emptyItem) {
        Preconditions.checkArgument(insertedItems && !insertedPaddingItems);
        // 插入的空元素不能为存在的元素
        Preconditions.checkArgument(!contains(emptyItem));
        for (int binIndex = 0; binIndex < binNum(); binIndex++) {
            // 如果相应的桶为空，则随便添加一个元素
            if (bins[binIndex] == null) {
                bins[binIndex] = HashBinEntry.fromEmptyItem(emptyItem);
                paddingItemSize++;
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
        Arrays.fill(bins, null);
        paddingItemSize = 0;
        itemSize = 0;
        insertedPaddingItems = false;
        insertedItems = false;
    }
}

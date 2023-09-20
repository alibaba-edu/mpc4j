package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.util.Arrays;

/**
 * 整数布谷鸟哈希抽象类。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
abstract class AbstractIntNoStashCuckooHashBin implements IntNoStashCuckooHashBin {
    /**
     * 整数布谷鸟哈希类型
     */
    private final IntCuckooHashBinType type;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * 最大元素数量
     */
    private final int maxItemSize;
    /**
     * 哈希桶数量
     */
    private final int binNum;
    /**
     * 哈希函数
     */
    private final Prf[] hashes;
    /**
     * 桶
     */
    private final int[] bins;
    /**
     * 插入元素对应的哈希值
     */
    private final int[] binHashIndexes;
    /**
     * 哈希桶中存储的元素数量
     */
    private int itemSize;
    /**
     * 哈希桶是否已经插入了元素
     */
    private boolean insertedItems;

    /**
     * 初始化整数布谷鸟哈希。
     *
     * @param envType     密码学组件。
     * @param maxItemSize 期望插入的最大元素数量。
     * @param keys        哈希函数密钥，如果为空则初始化新的密钥。
     */
    AbstractIntNoStashCuckooHashBin(EnvType envType, IntCuckooHashBinType type, int maxItemSize, byte[][] keys) {
        this(envType, type, maxItemSize, IntCuckooHashBinFactory.getBinNum(type, maxItemSize), keys);
    }

    AbstractIntNoStashCuckooHashBin(EnvType envType, IntCuckooHashBinType type, int maxItemSize, int binNum, byte[][] keys) {
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
        bins = new int[binNum];
        Arrays.fill(bins, -1);
        binHashIndexes = new int[binNum];
        Arrays.fill(binHashIndexes, -1);
        insertedItems = false;
    }

    @Override
    public IntCuckooHashBinType getType() {
        return type;
    }

    @Override
    public int getHashNum() {
        return hashNum;
    }

    @Override
    public byte[][] getHashKeys() {
        return Arrays.stream(hashes)
            .map(Prf::getKey)
            .toArray(byte[][]::new);
    }

    @Override
    public void insertItems(int[] items) {
        Preconditions.checkArgument(!insertedItems);
        // 一次插入的元素数量小于等于预先设定好的数量
        MathPreconditions.checkNonNegativeInRangeClosed("item.length", items.length, maxItemSize);
        long distinctCount = Arrays.stream(items)
            .peek(item -> MathPreconditions.checkNonNegative("item", item))
            .distinct()
            .count();
        Preconditions.checkArgument(distinctCount == items.length, "Inserted items contain duplicate item");
        for (int item : items) {
            insertItem(item, 0, 0);
        }
        insertedItems = true;
    }

    private void insertItem(int item, int hashIndex, int totalTries) {
        if (totalTries > IntCuckooHashBinFactory.DEFAULT_MAX_TOTAL_TRIES) {
            int currentItemSize = itemSize;
            clear();
            throw new ArithmeticException(
                String.format("Failed to insert items after %s items, no position to put by %s tries",
                    currentItemSize, IntCuckooHashBinFactory.DEFAULT_MAX_TOTAL_TRIES
                )
            );
        } else {
            // 如果没有超过最大迭代次数，则继续迭代
            totalTries++;
            int binIndex = hashes[hashIndex].getInteger(IntUtils.intToByteArray(item), binNum);
            int existItem = bins[binIndex];
            if (existItem < 0) {
                // 如果binIndex对应的位置无数据，则将当前的数据放置在binIndex中
                bins[binIndex] = item;
                binHashIndexes[binIndex] = hashIndex;
                itemSize++;
            } else {
                // 如果binIndex对应的数据不为空，则把这部分数据取出来，重新放置到另一个binIndex里面
                int evictItem = bins[binIndex];
                int evictItemHashIndex = binHashIndexes[binIndex];
                bins[binIndex] = item;
                binHashIndexes[binIndex] = hashIndex;
                insertItem(evictItem, ((evictItemHashIndex + 1) % hashNum), totalTries);
            }
        }
    }

    @Override
    public boolean insertedItems() {
        return insertedItems;
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
    public boolean contains(int item) {
        // 判断不同哈希函数对应的桶是否包含给定的元素
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            int binIndex = hashes[hashIndex].getInteger(IntUtils.intToByteArray(item), binNum);
            if (bins[binIndex] < 0) {
                continue;
            }
            if (bins[binIndex] == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getBinEntry(int binIndex) {
        return bins[binIndex];
    }

    @Override
    public int binNum() {
        return binNum;
    }

    @Override
    public int getBinHashIndex(int binIndex) {
        return binHashIndexes[binIndex];
    }

    @Override
    public void clear() {
        Arrays.fill(bins, -1);
        Arrays.fill(binHashIndexes, -1);
        itemSize = 0;
        insertedItems = false;
    }
}

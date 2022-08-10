package edu.alibaba.mpc4j.common.tool.hashbin.primitive;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 简单整数哈希桶。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public class SimpleIntHashBin implements IntHashBin {
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
     * 哈希桶
     */
    private final int[][] bins;
    /**
     * 哈希桶对应的哈希位置
     */
    private final int[][] binsHashIndexes;
    /**
     * 哈希桶元素数量
     */
    private final int[] binSize;
    /**
     * 元素哈希位置映射
     */
    private final Map<Integer, int[]> itemBinIndexesMap;

    /**
     * 初始化简单整数哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxItemSize 元素总个数。
     * @param keys 哈希密钥。
     */
    public SimpleIntHashBin(EnvType envType, int binNum, int maxItemSize, byte[][] keys) {
        this(envType, binNum, MaxBinSizeUtils.expectMaxBinSize(keys.length * maxItemSize, binNum), maxItemSize, keys);
    }

    /**
     * 初始化简单整数哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxBinSize  哈希桶最大元素个数。
     * @param maxItemSize 元素总个数。
     * @param keys 哈希密钥。
     */
    public SimpleIntHashBin(EnvType envType, int binNum, int maxBinSize, int maxItemSize, byte[][] keys) {
        assert binNum > 0;
        assert maxBinSize > 0;
        assert maxItemSize > 0;
        // 哈希数量乘以最大元素数量 <= 桶数量 * 每个桶的最大数
        assert keys.length * maxItemSize <= binNum * maxBinSize;
        this.binNum = binNum;
        this.maxBinSize = maxBinSize;
        this.maxItemSize = maxItemSize;
        itemSize = 0;
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
            .mapToObj(binIndex -> {
                int[] bin = new int[maxBinSize];
                Arrays.fill(bin, -1);
                return bin;
            })
            .toArray(int[][]::new);
        binsHashIndexes = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                int[] bin = new int[maxBinSize];
                Arrays.fill(bin, -1);
                return bin;
            })
            .toArray(int[][]::new);
        binSize = new int[binNum];
        // 初始化元素哈希映射
        itemBinIndexesMap = new HashMap<>(maxItemSize);
        insertedItems = false;
    }

    /**
     * 返回哈希数量。
     *
     * @return 哈希数量。
     */
    public int getHashNum() {
        return hashes.length;
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
    public void insertItems(int[] items) {
        assert !insertedItems;
        // 一次插入的元素数量要小于等于预先设定好的数量
        assert items.length <= maxItemSize;
        long distinctCount = Arrays.stream(items)
            .peek(item -> {
                assert item >= 0 : "Item must be non-negative: " + item;
            })
            .distinct().count();
        Preconditions.checkArgument(distinctCount == items.length, "Inserted items contain duplicate item");
        for (int item : items) {
            int[] itemBinIndexes = new int[hashes.length];
            for (int hashIndex = 0; hashIndex < hashes.length; hashIndex++) {
                // 遍历所有的哈希函数，对插入的元素求哈希
                int binIndex = hashes[hashIndex].getInteger(IntUtils.intToByteArray(item), binNum);
                itemBinIndexes[hashIndex] = binIndex;
                // 将元素插入到对应哈希桶中，可以包含重复元素
                if (binSize[binIndex] < maxBinSize) {
                    bins[binIndex][binSize[binIndex]] = item;
                    binsHashIndexes[binIndex][binSize[binIndex]] = hashIndex;
                    binSize[binIndex]++;
                    itemSize++;
                } else {
                    clear();
                    // 如果没有成功在哈希桶中插入元素，意味着桶超过最大值
                    throw new ArithmeticException(
                        String.format("bin[%s] contains %s items, exceeding MaxBinSize = %s",
                            binIndex, binSize[binIndex], maxBinSize
                        )
                    );
                }
            }
            itemBinIndexesMap.put(item, itemBinIndexes);
        }
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
    public boolean contains(int item) {
        return itemBinIndexesMap.containsKey(item);
    }

    /**
     * 返回元素所在桶的位置。
     *
     * @param item 元素。
     * @return 元素所在桶的位置。如果
     */
    public int[] getItemBinIndexes(int item) {
        assert insertedItems;
        assert item >= 0 : "Item must be non-negative: " + item;
        return itemBinIndexesMap.get(item);
    }

    @Override
    public int[] getBin(int binIndex) {
        return bins[binIndex];
    }

    @Override
    public int[] getBinHashIndexes(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return binsHashIndexes[binIndex];
    }

    @Override
    public int binSize(int binIndex) {
        assert binIndex >= 0 && binIndex < binNum;
        return binSize[binIndex];
    }

    @Override
    public void clear() {
        // 清空桶中的元素
        for (int[] bin : bins) {
            Arrays.fill(bin, -1);
        }
        for (int[] binHashIndexes : binsHashIndexes) {
            Arrays.fill(binHashIndexes, -1);
        }
        Arrays.fill(binSize, 0);
        // 清空索引值
        itemBinIndexesMap.clear();
        itemSize = 0;
        insertedItems = false;
    }
}

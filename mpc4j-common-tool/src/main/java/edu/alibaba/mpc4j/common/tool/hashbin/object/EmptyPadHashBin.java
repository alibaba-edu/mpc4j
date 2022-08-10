package edu.alibaba.mpc4j.common.tool.hashbin.object;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 填充空元素的哈希桶。主要用于下述论文的PSU方案中：
 * Kolesnikov, Vladimir, Mike Rosulek, Ni Trieu, and Xiao Wang. Scalable private set union from symmetric-key
 * techniques. ASIACRYPT 2019, pp. 636-666. Springer, Cham, 2019.
 *
 * @author Weiran Liu
 * @date 2021/12/23
 */
public class EmptyPadHashBin<T> implements HashBin<T> {
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
     * 哈希桶
     */
    private final ArrayList<ArrayList<HashBinEntry<T>>> bins;
    /**
     * 集合桶，用于快速判断桶中是否包含指定的元素
     */
    private final ArrayList<Set<HashBinEntry<T>>> setBins;
    /**
     * 元素哈希位置映射
     */
    private final Map<T, int[]> itemBinIndexesMap;

    /**
     * 初始化简单哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxItemSize 元素总个数。
     * @param keys 哈希密钥。
     */
    public EmptyPadHashBin(EnvType envType, int binNum, int maxItemSize, byte[][] keys) {
        this(envType, binNum, MaxBinSizeUtils.expectMaxBinSize(keys.length * maxItemSize, binNum), maxItemSize, keys);
    }

    /**
     * 初始化简单哈希桶。
     *
     * @param envType     环境类型。
     * @param binNum      哈希桶数量。
     * @param maxBinSize  哈希桶最大元素个数。
     * @param maxItemSize 元素总个数。
     * @param keys 哈希密钥。
     */
    public EmptyPadHashBin(EnvType envType, int binNum, int maxBinSize, int maxItemSize, byte[][] keys) {
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
            .mapToObj(binIndex -> {
                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(maxBinSize);
                bin.ensureCapacity(maxBinSize);
                return bin;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        setBins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> new HashSet<HashBinEntry<T>>(maxBinSize))
            .collect(Collectors.toCollection(ArrayList::new));
        // 初始化元素哈希映射
        itemBinIndexesMap = new HashMap<>(maxItemSize);
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
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
    public void insertItems(Collection<T> items) {
        assert (!insertedItems && !insertedPaddingItems);
        // 一次插入的元素数量小于等于预先设定好的数量
        assert items.size() <= maxItemSize;
        itemSize = 0;
        for (T item : items) {
            // 遍历所有的哈希函数，对插入的元素求哈希，并插入到对应的桶中
            int[] binIndexes = Arrays.stream(hashes)
                .mapToInt(hash -> hash.getInteger(ObjectUtils.objectToByteArray(item), binNum))
                .toArray();
            itemBinIndexesMap.put(item, binIndexes);
            for (int hashIndex = 0; hashIndex < hashes.length; hashIndex++) {
                HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, item);
                // 桶添加元素
                ArrayList<HashBinEntry<T>> bin = bins.get(binIndexes[hashIndex]);
                Set<HashBinEntry<T>> setBin = setBins.get(binIndexes[hashIndex]);
                if (setBin.add(hashBinEntry)) {
                    bin.add(hashBinEntry);
                    itemSize++;
                } else {
                    clear();
                    // 如果没有成功在哈希桶中插入元素，意味着输入的数据中包含重复元素
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

    /**
     * 返回元素所在桶的位置。
     *
     * @param item 元素。
     * @return 元素所在桶的位置。
     */
    public int[] getItemBinIndexes(T item) {
        assert insertedItems && itemBinIndexesMap.containsKey(item);
        return itemBinIndexesMap.get(item);
    }

    @Override
    public boolean contains(T item) {
        // 只要第0个哈希所对应的BinHashItem在简单哈希里面，就意味着简单哈希包含此元素
        HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(0, item);
        int binIndex = hashes[0].getInteger(hashBinEntry.getItemByteArray(), binNum);
        return setBins.get(binIndex).contains(hashBinEntry);
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
    public List<HashBinEntry<T>> getBin(int binIndex) {
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
    public void insertPaddingItems(T emptyItem) {
        assert (insertedItems && !insertedPaddingItems);
        // 桶中插入随机元素
        paddingItemSize = 0;
        IntStream.range(0, binNum).forEach(binIndex -> {
            Set<HashBinEntry<T>> setBin = setBins.get(binIndex);
            HashBinEntry<T> emptyHashBinEntry = HashBinEntry.fromEmptyItem(emptyItem);
            if (setBin.contains(emptyHashBinEntry)) {
                throw new IllegalArgumentException(String.format(
                    "bin[%s] already contains empty item %s", binIndex, emptyItem
                ));
            }
            // 在列表桶中插入空元素
            ArrayList<HashBinEntry<T>> bin = bins.get(binIndex);
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
        for (List<HashBinEntry<T>> bin : bins) {
            bin.clear();
        }
        // 清空哈希桶中的元素
        for (Set<HashBinEntry<T>> setBin : setBins) {
            setBin.clear();
        }
        itemBinIndexesMap.clear();
        itemSize = 0;
        paddingItemSize = 0;
        insertedItems = false;
        insertedPaddingItems = false;
    }
}

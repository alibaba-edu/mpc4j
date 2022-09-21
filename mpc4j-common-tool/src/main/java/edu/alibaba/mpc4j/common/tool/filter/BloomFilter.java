package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 布隆过滤器实现，代码参考了Google Guava的实现代码：
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/BloomFilter.java
 * 理论推导参考了论文：
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol.
 * CCS 2013, pp. 789-800.
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class BloomFilter<T> implements MergeFilter<T> {
    /**
     * 当布隆过滤器的最优比特数量m = n log_2(e) * log_2(1/p)时，布隆过滤器的最优哈希函数数量 = log_2(1/p)，即安全常数
     */
    static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;

    /**
     * 计算布隆过滤器的比特数量m，失效率默认为统计安全常数。
     *
     * @param n 期望插入的元素数量。
     * @return 布隆过滤器的最优比特数量m，此数量可以被{@code Byte.SIZE}整除。
     */
    public static int bitSize(int n) {
        int bitLength = (int)Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2));
        return CommonUtils.getByteLength(bitLength) * Byte.SIZE;
    }

    /**
     * 插入的最大元素数量
     */
    private int maxSize;
    /**
     * 哈希函数的输出范围
     */
    private int m;
    /**
     * 已经插入的元素数量
     */
    private int size;
    /**
     * 用字节数组表示的布隆过滤器
     */
    private byte[] bloomFilterBytes;
    /**
     * 布隆过滤器的哈希函数
     */
    private Prf[] hashes;
    /**
     * 原始元素的字节长度，用于计算压缩比例
     */
    private int itemByteLength;

    /**
     * 创建一个空的布隆过滤器。
     *
     * @param envType 环境类型。
     * @param maxSize 插入的最大元素数量。
     * @param keys    哈希密钥。
     * @return 空的布隆过滤器。
     */
    public static <X> BloomFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        assert maxSize > 0;
        assert keys.length == HASH_NUM;
        BloomFilter<X> bloomFilter = new BloomFilter<>();
        bloomFilter.maxSize = maxSize;
        bloomFilter.m = BloomFilter.bitSize(bloomFilter.maxSize);
        bloomFilter.bloomFilterBytes = new byte[bloomFilter.m / Byte.SIZE];
        bloomFilter.hashes = Arrays.stream(keys).map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
        // 将布隆过滤器初始化为全0
        Arrays.fill(bloomFilter.bloomFilterBytes, (byte)0x00);
        bloomFilter.size = 0;
        bloomFilter.itemByteLength = 0;

        return bloomFilter;
    }

    /**
     * 将用{@code List<byte[]>}表示的过滤器转换为布隆过滤器。
     *
     * @param envType 环境类型。
     * @param byteArrayList     用{@code List<byte[]>}表示的过滤器。
     * @param <X> 过滤器存储元素类型。
     * @return 布隆过滤器。
     */
    static <X> BloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() == 5 + HASH_NUM);
        BloomFilter<X> bloomFilter = new BloomFilter<>();
        // 移除过滤器类型
        byteArrayList.remove(0);
        // 期望插入的元素数量
        bloomFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        bloomFilter.m = BloomFilter.bitSize(bloomFilter.maxSize);
        // 已经插入的元素数量
        bloomFilter.size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 原始元素的字节长度
        bloomFilter.itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 当前存储状态
        bloomFilter.bloomFilterBytes = byteArrayList.remove(0);
        // 密钥
        byte[][] keys = byteArrayList.toArray(new byte[0][]);
        bloomFilter.hashes = Arrays.stream(keys).map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);

        return bloomFilter;
    }

    private BloomFilter() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // 过滤器类型
        byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // 预计插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(maxSize()));
        // 已经插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(size()));
        // 原始元素的字节长度
        byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
        // 当前存储状态
        byteArrayList.add(BytesUtils.clone(bloomFilterBytes));
        // 密钥
        for (Prf hash : hashes) {
            byteArrayList.add(BytesUtils.clone(hash.getKey()));
        }

        return byteArrayList;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.BLOOM_FILTER;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public boolean mightContain(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        // 依次验证每个哈希结果所对应的比特位是否为true
        for (Prf hash : hashes) {
            int index = hash.getInteger(objectBytes, m);
            if (!BinaryUtils.getBoolean(bloomFilterBytes, index)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized void put(T data) {
        assert size < maxSize;
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        // mightContain已经检查了重复元素，直接插入
        int[] indexes = Arrays.stream(hashes)
            .mapToInt(hash -> hash.getInteger(objectBytes, m))
            .distinct()
            .toArray();
        for (int index : indexes) {
            if (!BinaryUtils.getBoolean(bloomFilterBytes, index)) {
                BinaryUtils.setBoolean(bloomFilterBytes, index, true);
            }
        }
        // 更新存储信息
        itemByteLength += objectBytes.length;
        size++;
    }

    @Override
    public void merge(MergeFilter<T> otherFilter) {
        assert otherFilter instanceof BloomFilter;
        BloomFilter<T> otherBloomFilter = (BloomFilter<T>)otherFilter;
        // 预计插入的元素数量应该一致，否则哈希长度不一致
        assert maxSize == otherBloomFilter.maxSize;
        IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
            // 哈希函数的类型相同
            assert hashes[hashIndex].getPrfType().equals(otherBloomFilter.hashes[hashIndex].getPrfType());
            // 哈希函数的密钥相同
            assert Arrays.equals(hashes[hashIndex].getKey(), otherBloomFilter.hashes[hashIndex].getKey());
        });
        assert maxSize >= size + otherBloomFilter.size;
        // 合并布隆过滤器
        BytesUtils.ori(bloomFilterBytes, otherBloomFilter.bloomFilterBytes);
        size += otherBloomFilter.size;
        itemByteLength += otherBloomFilter.itemByteLength;
    }

    @Override
    public double ratio() {
        return (double)bloomFilterBytes.length / itemByteLength;
    }

    /**
     * 返回布隆过滤器存储的字节数组。
     *
     * @return 布隆过滤器存储的字节数组。
     */
    public byte[] getBytes() {
        return bloomFilterBytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        BloomFilter<T> that = (BloomFilter<T>)obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.bloomFilterBytes, that.bloomFilterBytes);
        IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
            equalsBuilder.append(hashes[hashIndex].getPrfType(), that.hashes[hashIndex].getPrfType());
            equalsBuilder.append(hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey());
        });
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(bloomFilterBytes);
        IntStream.range(0, HASH_NUM).forEach(hashIndex -> {
            hashCodeBuilder.append(hashes[hashIndex].getPrfType());
            hashCodeBuilder.append(hashes[hashIndex].getKey());
        });
        return hashCodeBuilder.toHashCode();
    }
}

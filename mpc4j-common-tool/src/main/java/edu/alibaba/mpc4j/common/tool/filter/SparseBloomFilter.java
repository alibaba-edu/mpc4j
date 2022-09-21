package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 稀疏过滤器，在RR16的PSI中使用，以实现恶意安全PSI协议。与Bloom Filter相比，Spare Bloom Filter更加稀疏。论文来源：
 * Rindal P, Rosulek M. Improved private set intersection against malicious adversaries.
 * EUROCRYPT 2017. Springer, Cham, 2017: 235-259.
 *
 * @author Ziyuan Liang, Weiran Liu
 * @date 2020/09/30
 */
public class SparseBloomFilter<T> implements MergeFilter<T> {
    /**
     * 最大插入元素数量与过滤器比特长度间的关系
     */
    private static final Map<Integer, Integer> SBF_BIT_LENGTH_INIT_MATRIX = new HashMap<>();

    static {
        SBF_BIT_LENGTH_INIT_MATRIX.put(8, 88031);
        SBF_BIT_LENGTH_INIT_MATRIX.put(9, 159945);
        SBF_BIT_LENGTH_INIT_MATRIX.put(10, 303464);
        SBF_BIT_LENGTH_INIT_MATRIX.put(11, 579993);
        SBF_BIT_LENGTH_INIT_MATRIX.put(12, 1120665);
        SBF_BIT_LENGTH_INIT_MATRIX.put(13, 2181857);
        SBF_BIT_LENGTH_INIT_MATRIX.put(14, 4270964);
        SBF_BIT_LENGTH_INIT_MATRIX.put(15, 8402960);
        SBF_BIT_LENGTH_INIT_MATRIX.put(16, 16579297);
        SBF_BIT_LENGTH_INIT_MATRIX.put(17, 32836550);
        SBF_BIT_LENGTH_INIT_MATRIX.put(18, 65163755);
        SBF_BIT_LENGTH_INIT_MATRIX.put(19, 129392705);
        SBF_BIT_LENGTH_INIT_MATRIX.put(20, 257635123);
        SBF_BIT_LENGTH_INIT_MATRIX.put(21, 513277951);
        SBF_BIT_LENGTH_INIT_MATRIX.put(22, 1023879938);
        SBF_BIT_LENGTH_INIT_MATRIX.put(23, 2042206617);
    }

    /**
     * 稀疏布隆过滤器的最优比特数量m（论文中的符号是N_bf），失效率默认为统计安全常数。
     *
     * @param maxSize 期望插入的元素数量。
     * @return 布隆过滤器的最优比特数量m，此数量可以被{@code Byte.SIZE}整除。
     */
    public static int bitSize(int maxSize) {
        assert maxSize > 0;
        int nLogValue = Math.max(8, LongUtils.ceilLog2(maxSize));
        if (nLogValue > 23) {
            throw new IllegalArgumentException("插入元素数量 = " + maxSize + "，超过支持的最大数量 = " + (1 << 23));
        }
        return SBF_BIT_LENGTH_INIT_MATRIX.get(nLogValue);
    }

    /**
     * 最大插入元素数量与哈希函数数量的关系
     */
    private static final Map<Integer, Integer> HASH_NUM_INIT_MATRIX = new HashMap<>();

    static {
        HASH_NUM_INIT_MATRIX.put(8, 105);
        HASH_NUM_INIT_MATRIX.put(9, 101);
        HASH_NUM_INIT_MATRIX.put(10, 98);
        HASH_NUM_INIT_MATRIX.put(11, 96);
        HASH_NUM_INIT_MATRIX.put(12, 95);
        HASH_NUM_INIT_MATRIX.put(13, 92);
        HASH_NUM_INIT_MATRIX.put(14, 93);
        HASH_NUM_INIT_MATRIX.put(15, 91);
        HASH_NUM_INIT_MATRIX.put(16, 91);
        HASH_NUM_INIT_MATRIX.put(17, 91);
        HASH_NUM_INIT_MATRIX.put(18, 90);
        HASH_NUM_INIT_MATRIX.put(19, 89);
        HASH_NUM_INIT_MATRIX.put(20, 90);
        HASH_NUM_INIT_MATRIX.put(21, 89);
        HASH_NUM_INIT_MATRIX.put(22, 88);
        HASH_NUM_INIT_MATRIX.put(23, 90);
    }

    /**
     * 返回稀疏布隆过滤器的哈希函数数量，来自于论文表5。
     *
     * @param maxSize 最大插入元素数量。
     * @return 哈希函数数量。
     */
    public static int getHashNum(int maxSize) {
        assert maxSize > 0;
        int nLogValue = Math.max(8, LongUtils.ceilLog2(maxSize));
        if (nLogValue > 23) {
            throw new IllegalArgumentException("插入元素数量 = " + maxSize + "，超过支持的最大数量 = " + (1 << 23));
        }
        return HASH_NUM_INIT_MATRIX.get(nLogValue);
    }

    /**
     * 哈希函数数量
     */
    private int hashNum;
    /**
     * 期望插入的元素数量
     */
    private int maxSize;
    /**
     * 哈希函数的输出范围，即稀疏布隆过滤器的比特长度
     */
    private int m;
    /**
     * 用字节数组表示的稀疏布隆过滤器
     */
    private byte[] sparseBloomFilterBytes;
    /**
     * 稀疏布隆过滤器的哈希函数
     */
    private Prf[] hashes;
    /**
     * 已经插入的元素数量
     */
    private int size;
    /**
     * 原始元素的字节长度，用于计算压缩比例
     */
    private int itemByteLength;

    /**
     * 构造一个空的稀疏布隆过滤器。
     *
     * @param maxSize 期望插入的元素数量。
     * @param keys    哈希函数的密钥。
     * @return 空的稀疏布隆过滤器。
     */
    public static <X> SparseBloomFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        assert maxSize > 0;
        SparseBloomFilter<X> sparseBloomFilter = new SparseBloomFilter<>();
        sparseBloomFilter.maxSize = maxSize;
        // 计算稀疏布隆过滤器的最优比特数量
        sparseBloomFilter.m = bitSize(maxSize);
        // 计算稀疏布隆过滤器的最优哈希数量
        sparseBloomFilter.hashNum = getHashNum(maxSize);
        assert keys.length == sparseBloomFilter.hashNum;
        sparseBloomFilter.sparseBloomFilterBytes = new byte[CommonUtils.getByteLength(sparseBloomFilter.m)];
        sparseBloomFilter.hashes = Arrays.stream(keys)
            .map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
        // 初始化时布隆过滤器为空
        Arrays.fill(sparseBloomFilter.sparseBloomFilterBytes, (byte)0x00);
        sparseBloomFilter.size = 0;
        sparseBloomFilter.itemByteLength = 0;

        return sparseBloomFilter;
    }

    /**
     * 将用{@code List<byte[]>}表示的过滤器转换为稀疏布隆过滤器。
     *
     * @param envType 环境类型。
     * @param filterList  用{@code List<byte[]>}表示的过滤器。
     * @return 稀疏布隆过滤器。
     */
    static <X> SparseBloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> filterList) {
        Preconditions.checkArgument(filterList.size() >= 5);
        SparseBloomFilter<X> sparseBloomFilter = new SparseBloomFilter<>();
        // 移除过滤器类型
        filterList.remove(0);
        // 期望插入的元素数量
        sparseBloomFilter.maxSize = IntUtils.byteArrayToInt(filterList.remove(0));
        sparseBloomFilter.m = SparseBloomFilter.bitSize(sparseBloomFilter.maxSize);
        sparseBloomFilter.hashNum = getHashNum(sparseBloomFilter.maxSize);
        // 已经插入的元素数量
        sparseBloomFilter.size = IntUtils.byteArrayToInt(filterList.remove(0));
        // 原始数据的字节长度
        sparseBloomFilter.itemByteLength = IntUtils.byteArrayToInt(filterList.remove(0));
        // 当前存储状态
        sparseBloomFilter.sparseBloomFilterBytes = filterList.remove(0);
        assert filterList.size() == sparseBloomFilter.hashNum;
        // 密钥
        byte[][] keys = filterList.toArray(new byte[0][]);
        sparseBloomFilter.hashes = Arrays.stream(keys).map(key -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);

        return sparseBloomFilter;
    }


    private SparseBloomFilter() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // 过滤器类型
        byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // 预计插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(maxSize));
        // 已经插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(size));
        // 原始元素的字节长度
        byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
        // 当前存储状态
        byteArrayList.add(BytesUtils.clone(sparseBloomFilterBytes));
        // 密钥
        for (Prf hash : hashes) {
            byteArrayList.add(BytesUtils.clone(hash.getKey()));
        }
        return byteArrayList;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return FilterFactory.FilterType.SPARSE_BLOOM_FILTER;
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
            if (!BinaryUtils.getBoolean(sparseBloomFilterBytes, index)) {
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
            if (!BinaryUtils.getBoolean(sparseBloomFilterBytes, index)) {
                BinaryUtils.setBoolean(sparseBloomFilterBytes, index, true);
            }
        }
        // 更新存储信息
        itemByteLength += objectBytes.length;
        size++;
    }

    @Override
    public void merge(MergeFilter<T> otherFilter) {
        assert otherFilter instanceof SparseBloomFilter;
        SparseBloomFilter<T> otherBloomFilter = (SparseBloomFilter<T>)otherFilter;
        // 预计插入的元素数量应该一致，否则哈希长度不一致
        assert maxSize == otherBloomFilter.maxSize;
        IntStream.range(0, hashNum).forEach(hashIndex -> {
            // 哈希函数的类型相同
            assert hashes[hashIndex].getPrfType().equals(otherBloomFilter.hashes[hashIndex].getPrfType());
            // 哈希函数的密钥相同
            assert Arrays.equals(hashes[hashIndex].getKey(), otherBloomFilter.hashes[hashIndex].getKey());
        });
        assert maxSize >= size + otherBloomFilter.size;
        // 合并布隆过滤器
        BytesUtils.ori(sparseBloomFilterBytes, otherBloomFilter.sparseBloomFilterBytes);
        size += otherBloomFilter.size;
        itemByteLength += otherBloomFilter.itemByteLength;
    }

    @Override
    public double ratio() {
        return (double)sparseBloomFilterBytes.length / itemByteLength;
    }

    /**
     * 返回布隆过滤器存储的字节数组
     *
     * @return 布隆过滤器存储的字节数组
     */
    public byte[] getBytes() {
        return sparseBloomFilterBytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseBloomFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        SparseBloomFilter<T> that = (SparseBloomFilter<T>)obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.sparseBloomFilterBytes, that.sparseBloomFilterBytes);
        IntStream.range(0, this.hashNum).forEach(hashIndex -> {
            equalsBuilder.append(this.hashes[hashIndex].getPrfType(), that.hashes[hashIndex].getPrfType());
            equalsBuilder.append(this.hashes[hashIndex].getKey(), that.hashes[hashIndex].getKey());
        });
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(this.maxSize)
            .append(this.size)
            .append(this.itemByteLength)
            .append(this.sparseBloomFilterBytes);
        IntStream.range(0, this.hashNum).forEach(hashIndex -> {
            hashCodeBuilder.append(this.hashes[hashIndex].getPrfType());
            hashCodeBuilder.append(this.hashes[hashIndex].getKey());
        });
        return hashCodeBuilder.toHashCode();
    }
}

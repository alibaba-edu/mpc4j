package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cuckoo Filter的实现，原始论文：
 * Fan B, Andersen D G, Kaminsky M, et al. Cuckoo filter: Practically better than bloom. CoNET 2014, pp. 75-88.
 * 与Bloom Filter相比，Cuckoo Filter有如下优势：
 * 1. 支持动态添加和删除元素（Support adding and removing items dynamically.）。
 * 2. 更高效的元素存在性检查（Provide higher lookup performance than traditional Bloom Filters.）。
 * 3. 很多实际场景中存储量低（Use less space than Bloom Filters in many practical applications.）。
 *
 * @author FengQing, Weiran Liu
 * @date 2020/08/29
 */
public class CuckooFilter<T> implements Filter<T> {
    /**
     * 负载率α，即插入期望的所有元素后，哈希桶中包含的元素数量占总元素数量的比例，论文给出的默认比例是95.5%，见Table 2
     */
    private static final double LOAD_FACTOR = 0.955;
    /**
     * 每个桶内有多少个元素（entry），这里使用默认值4
     */
    private static final int ENTRIES_PER_BUCKET = 4;
    /**
     * 每个指纹（fingerprint）的比特长度，计算公式(log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET))
     * 由于ENTRIES_PER_BUCKET = 4，log_2(1/ε) = 40，因此计算结果为40 + 3  = 43，取到Byte.SIZE整除，即48
     * 参见论文Table 2
     */
    static final int FINGERPRINT_BYTE_LENGTH = CommonUtils.getByteLength(
        DoubleMath.roundToInt((CommonConstants.STATS_BIT_LENGTH + 3), RoundingMode.UP)
    );
    /**
     * 发生冲突时，踢出元素的最大次数，论文中的设置为500。经测试（插入2^20个元素），该值可能会在正常负载率水平时引起插入失败。
     * 若踢出次数较小，则插入元素的开销会减小，但会达不到理想的"元素在桶内分布均匀"的状态，从而增加元素插入失败概率，进而使得负载率变低。
     * 若踢出次数较大，则减小元素插入失败概率，负载率变高，但在负载率达到较大水平后，插入元素时的开销会增加（需踢出更多元素才能找到位置）。
     * 论文中未讨论该值影响，结合实际测试结果，与布谷鸟哈希设置的最大次数保持一致，为2^10 = 1024。
     */
    private static final int MAX_NUM_KICKS = 1 << 10;
    /**
     * 空的字节缓存区
     */
    private static final ByteBuffer ZERO_BYTE_BUFFER = ByteBuffer.wrap(new byte[0]);
    /**
     * 哈希函数密钥数量
     */
    static final int HASH_NUM = 2;

    /**
     * 给定布谷鸟过滤器插入的元素数量，返回哈希桶数量，哈希桶数量一定是2^k形式。
     *
     * @param maxSize 插入最大元素数量。
     * @return 哈希桶数量。
     */
    private static int getBucketNum(int maxSize) {
        return 1 << LongUtils.ceilLog2(
            DoubleMath.roundToInt((1.0 / LOAD_FACTOR) * maxSize / ENTRIES_PER_BUCKET, RoundingMode.UP) + 1
        );
    }

    /**
     * 期望插入的元素数量
     */
    private int maxSize;
    /**
     * 桶（bucket）数量
     */
    private int bucketNum;
    /**
     * 随机状态
     */
    private SecureRandom secureRandom;
    /**
     * 布谷鸟过滤器桶
     */
    private ArrayList<ArrayList<CuckooFilterEntry>> buckets;
    /**
     * 用于计算布谷鸟过滤器哈希桶位置的哈希函数
     */
    private Prf bucketHash;
    /**
     * 用于计算插入元素指纹的哈希函数
     */
    private Prf fingerprintHash;
    /**
     * 已经插入的元素数量
     */
    private int size;
    /**
     * 原始元素的字节长度，用于计算压缩比例
     */
    private int itemByteLength;

    /**
     * 创建一个空的布谷鸟过滤器。
     *
     * @param envType 环境类型。
     * @param maxSize 期望插入的元素数量。
     * @param keys    哈希函数密钥。
     * @return 空的布谷鸟过滤器。
     */
    static <X> CuckooFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        assert maxSize > 0;
        CuckooFilter<X> cuckooFilter = new CuckooFilter<>();
        cuckooFilter.maxSize = maxSize;
        cuckooFilter.bucketNum = getBucketNum(cuckooFilter.maxSize);
        cuckooFilter.secureRandom = new SecureRandom();
        cuckooFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        cuckooFilter.fingerprintHash.setKey(keys[0]);
        cuckooFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        cuckooFilter.bucketHash.setKey(keys[1]);
        // 初始化哈希桶
        cuckooFilter.buckets = IntStream.range(0, cuckooFilter.bucketNum)
            .mapToObj(bucketIndex -> new ArrayList<CuckooFilterEntry>(ENTRIES_PER_BUCKET))
            .collect(Collectors.toCollection(ArrayList::new));
        cuckooFilter.size = 0;
        cuckooFilter.itemByteLength = 0;

        return cuckooFilter;
    }

    /**
     * 将用{@code List<byte[]>}表示的过滤器转换为布谷鸟过滤器。
     *
     * @param envType       环境类型。
     * @param byteArrayList 用{@code BigInteger<List>}表示的过滤器。
     * @return 布谷鸟过滤器。
     */
    static <X> CuckooFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 6);
        CuckooFilter<X> cuckooFilter = new CuckooFilter<>();
        // 移除过滤器类型
        byteArrayList.remove(0);
        // 期望插入的元素数量
        cuckooFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        cuckooFilter.bucketNum = getBucketNum(cuckooFilter.maxSize);
        // 已经插入的元素数量
        cuckooFilter.size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 原始数据的字节长度
        cuckooFilter.itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 指纹哈希密钥
        byte[] fingerprintHashKey = byteArrayList.remove(0);
        cuckooFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        cuckooFilter.fingerprintHash.setKey(fingerprintHashKey);
        // 桶哈希密钥
        byte[] bucketHashKey = byteArrayList.remove(0);
        cuckooFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        cuckooFilter.bucketHash.setKey(bucketHashKey);
        cuckooFilter.secureRandom = new SecureRandom();
        // 桶中的元素
        Preconditions.checkArgument(byteArrayList.size() == cuckooFilter.bucketNum * ENTRIES_PER_BUCKET);
        ByteBuffer[] bucketFlattenedElements = byteArrayList.stream().map(ByteBuffer::wrap).toArray(ByteBuffer[]::new);
        cuckooFilter.buckets = IntStream.range(0, cuckooFilter.bucketNum)
            .mapToObj(bucketIndex -> {
                ArrayList<CuckooFilterEntry> bucket = new ArrayList<>(ENTRIES_PER_BUCKET);
                IntStream.range(0, ENTRIES_PER_BUCKET).forEach(index -> {
                    // 把空标签设置为0
                    if (!bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + index].equals(ZERO_BYTE_BUFFER)) {
                        bucket.add(
                            new CuckooFilterEntry(bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + index])
                        );
                    }
                });
                return bucket;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        byteArrayList.clear();

        return cuckooFilter;
    }

    private CuckooFilter() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> cuckooFilterList = new LinkedList<>();
        // 过滤器类型
        cuckooFilterList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // 期望插入的元素数量
        cuckooFilterList.add(IntUtils.intToByteArray(maxSize));
        // 已经插入的元素数量
        cuckooFilterList.add(IntUtils.intToByteArray(size));
        // 原始数据的字节长度
        cuckooFilterList.add(IntUtils.intToByteArray(itemByteLength));
        // 指纹哈希密钥
        cuckooFilterList.add(BytesUtils.clone(fingerprintHash.getKey()));
        // 桶哈希密钥
        cuckooFilterList.add(BytesUtils.clone(bucketHash.getKey()));
        // 桶中的元素
        IntStream.range(0, bucketNum).forEach(bucketIndex -> {
            // 插入元素内容，空元素用0占位
            List<CuckooFilterEntry> bucket = buckets.get(bucketIndex);
            int remainSize = ENTRIES_PER_BUCKET - bucket.size();
            for (CuckooFilterEntry cuckooFilterEntry : bucket) {
                cuckooFilterList.add(BytesUtils.clone(cuckooFilterEntry.getFingerprint().array()));
            }
            while (remainSize > 0) {
                cuckooFilterList.add(new byte[0]);
                remainSize--;
            }
        });

        return cuckooFilterList;
    }

    @Override
    public FilterFactory.FilterType getFilterType() {
        return FilterFactory.FilterType.CUCKOO_FILTER;
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
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int fingerPrintHash = bucketHash.getInteger(fingerprint.array(), bucketNum);
        int bucketIndex2 = Math.abs((bucketIndex1 ^ fingerPrintHash) % bucketNum);
        CuckooFilterEntry entry = new CuckooFilterEntry(fingerprint);

        return buckets.get(bucketIndex1).contains(entry) || buckets.get(bucketIndex2).contains(entry);
    }

    @Override
    public synchronized void put(T data) {
        assert size < maxSize;
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int fingerprintHash = bucketHash.getInteger(fingerprint.array(), bucketNum);
        int bucketIndex2 = Math.abs((bucketIndex1 ^ fingerprintHash) % bucketNum);
        // if bucket[i_1] or bucket[i_2] has an empty entry, then add f to that bucket
        if (buckets.get(bucketIndex1).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex1).add(new CuckooFilterEntry(fingerprint));
            size++;
            itemByteLength += objectBytes.length;
        } else if (buckets.get(bucketIndex2).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex2).add(new CuckooFilterEntry(fingerprint));
            size++;
            itemByteLength += objectBytes.length;
        } else {
            // i = randomly pick i_1 or i_2
            int choiceIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            List<CuckooFilterEntry> choiceBucket = buckets.get(choiceIndex);
            byte[] addedFingerprintBytes = Arrays.copyOf(fingerprint.array(), fingerprint.array().length);
            byte[] choiceFingerprintBytes;
            int choiceEntryIndex;
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // randomly select an entry e from bucket[i]
                choiceEntryIndex = secureRandom.nextInt(ENTRIES_PER_BUCKET);
                // 将指纹值拷贝出来
                ByteBuffer choiceFingerprint = choiceBucket.remove(choiceEntryIndex).getFingerprint();
                choiceFingerprintBytes = choiceFingerprint.array();
                // 将待插入的指纹插入到新的位置中
                ByteBuffer copyAddedFingerprint = ByteBuffer.wrap(
                    Arrays.copyOf(addedFingerprintBytes, addedFingerprintBytes.length)
                );
                choiceBucket.add(new CuckooFilterEntry(copyAddedFingerprint));
                // 踢出再插入元素后，哈希桶中元素的数量应该仍然为ENTRIES_PER_BUCKET
                Preconditions.checkArgument(choiceBucket.size() == ENTRIES_PER_BUCKET);
                addedFingerprintBytes = BytesUtils.clone(choiceFingerprintBytes);
                int choiceFingerprintHash = bucketHash.getInteger(choiceFingerprintBytes, bucketNum);
                choiceIndex = Math.abs((choiceIndex ^ choiceFingerprintHash) % bucketNum);
                choiceBucket = buckets.get(choiceIndex);
                // bucket[i] has an empty entry, then add f to that bucket
                if (choiceBucket.size() < ENTRIES_PER_BUCKET) {
                    choiceBucket.add(new CuckooFilterEntry(ByteBuffer.wrap(addedFingerprintBytes)));
                    size++;
                    itemByteLength += objectBytes.length;
                    return;
                }
                // 如果到达这个位置，意味着踢出元素放置到新的哈希桶中后，新的哈希桶元素数量仍然达到ENTRIES_PER_BUCKET，需要重复踢出
            }
            // 如果到达这个位置，意味着不能再踢出元素了
            throw new IllegalArgumentException("Cannot add item, exceeding max tries: " + data);
        }
    }

    @Override
    public double ratio() {
        // 当前占用的字节长度等于已插入元素的数量乘以指纹长度，加上空占位符的数量
        int cuckooFilterByteLength = size * FINGERPRINT_BYTE_LENGTH
            + (bucketNum * ENTRIES_PER_BUCKET - size) * CommonUtils.getByteLength(1);
        return ((double)cuckooFilterByteLength) / itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CuckooFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        CuckooFilter<T> that = (CuckooFilter<T>)obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.fingerprintHash.getPrfType(), that.fingerprintHash.getPrfType())
            .append(this.fingerprintHash.getKey(), that.fingerprintHash.getKey())
            .append(this.bucketHash.getPrfType(), that.bucketHash.getPrfType())
            .append(this.bucketHash.getKey(), that.bucketHash.getKey());
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            equalsBuilder.append(
                new HashSet<>(this.buckets.get(buckedIndex)),
                new HashSet<>(that.buckets.get(buckedIndex))
            )
        );
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(fingerprintHash.getPrfType())
            .append(fingerprintHash.getKey())
            .append(bucketHash.getKey());
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new HashSet<>(buckets.get(buckedIndex))));
        return hashCodeBuilder.toHashCode();
    }
}

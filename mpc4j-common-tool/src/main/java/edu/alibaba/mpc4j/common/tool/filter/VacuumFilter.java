package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Vacuum Filter的实现，原始论文：
 * Wang M, Zhou M, Shi S, et al. Vacuum filters: more space-efficient and faster replacement for bloom and cuckoo
 * filters[J]. Proceedings of the VLDB Endowment, 2019, 13(2): 197-210.
 *
 * @author FengQing, Ziyuan Liang
 * @date 2020/10/21
 */
public class VacuumFilter<T> implements Filter<T> {
    /**
     * 负载率α，即插入期望的所有元素后，哈希桶中包含的元素数量占总元素数量的比例，论文给出的默认比例是95.5%，见Table 2
     */
    private static final double LOAD_FACTOR = 0.955;
    /**
     * 每个桶内有多少个元素（entry），这里使用默认值4
     */
    private static final int ENTRIES_PER_BUCKET = 4;
    /**
     * 索引值可选范围数量
     */
    private static final int ALTERNATE_RANGE_NUM = 4;
    /**
     * 每个指纹（fingerprint）的字节长度，计算公式(log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET))
     * 由于ENTRIES_PER_BUCKET = 4，log_2(1/ε) = 40，因此计算结果为40 + 3 = 43，取到Byte.SIZE整除，即48
     * 参见论文Table 2
     */
    static final int FINGERPRINT_BYTE_LENGTH = CommonUtils.getByteLength(
        DoubleMath.roundToInt((CommonConstants.STATS_BIT_LENGTH + 3), RoundingMode.UP)
    );
    /**
     * 空的字节缓存区
     */
    private static final ByteBuffer ZERO_BYTE_BUFFER = ByteBuffer.wrap(new byte[0]);
    /**
     * 最大踢出次数，设置为2^11 = 2048
     */
    private static final int MAX_NUM_KICKS = 1 << 11;
    /**
     * VacummFilter中的哈希函数数量
     */
    static final int HASH_KEY_NUM = 2;

    /**
     * 返回桶的数量。
     *
     * @param maxSize 插入最大元素数量。
     * @param list    4个可选的输出范围。
     */
    private static int getBucketNum(int maxSize, List<Integer> list) {
        int n = DoubleMath.roundToInt((1.0 / LOAD_FACTOR) * maxSize / ENTRIES_PER_BUCKET, RoundingMode.UP);
        return (n / list.get(0) + 1) * list.get(0);
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
     * 真空过滤器
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
     * 索引值可选范围
     */
    private ArrayList<Integer> alternateRange;

    /**
     * 初始化一个空的真空过滤器。
     *
     * @param envType 环境类型。
     * @param maxSize 插入最大元素数量。
     * @param keys    哈希函数密钥。
     * @return 空的真空过滤器。
     */
    static <X> VacuumFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        assert maxSize > 0;
        VacuumFilter<X> vacuumFilter = new VacuumFilter<>();
        // 先初始化this.maxSize，再initL()，最后初始化bucketNum
        vacuumFilter.maxSize = maxSize;
        vacuumFilter.alternateRange = new ArrayList<>(ALTERNATE_RANGE_NUM);
        // 初始化4个可选的输出index范围，分别在不同比例下执行4次rangeSelection()
        for (int i = 0; i < ENTRIES_PER_BUCKET; i++) {
            vacuumFilter.alternateRange.add(rangeSelection(maxSize, 1 - i * (i + 1) * 0.05));
        }
        // L[3] *= 2, 避免L[3]过小造成频繁插入失败
        vacuumFilter.alternateRange.set(3, vacuumFilter.alternateRange.get(3) * 2);
        vacuumFilter.bucketNum = getBucketNum(maxSize, vacuumFilter.alternateRange);
        vacuumFilter.secureRandom = new SecureRandom();
        vacuumFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        vacuumFilter.fingerprintHash.setKey(keys[0]);
        // 这里设置的哈希的输出范围仍然是2^n。
        vacuumFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        vacuumFilter.bucketHash.setKey(keys[1]);
        // 初始化哈希桶，注意要保证线程安全
        vacuumFilter.buckets = IntStream.range(0, vacuumFilter.bucketNum)
            .mapToObj(bucketIndex -> new ArrayList<CuckooFilterEntry>(ENTRIES_PER_BUCKET))
            .collect(Collectors.toCollection(ArrayList::new));
        vacuumFilter.size = 0;
        vacuumFilter.itemByteLength = 0;

        return vacuumFilter;
    }

    /**
     * 将用{@code List<byte[]>}表示的过滤器转换为真空过滤器。
     *
     * @param envType       环境类型。
     * @param byteArrayList 用{@code List<byte[]>}表示的过滤器。
     * @return 真空过滤器。
     */
    static <X> VacuumFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(byteArrayList.size() >= 6);
        VacuumFilter<X> vacuumFilter = new VacuumFilter<>();
        // 移除过滤器类型
        byteArrayList.remove(0);
        // 期望插入的元素数量
        vacuumFilter.maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        vacuumFilter.alternateRange = new ArrayList<>(ALTERNATE_RANGE_NUM);
        // 初始化4个可选的输出index范围，分别在不同比例下执行4次rangeSelection()
        for (int i = 0; i < ENTRIES_PER_BUCKET; i++) {
            vacuumFilter.alternateRange.add(rangeSelection(vacuumFilter.maxSize, 1 - i * (i + 1) * 0.05));
        }
        // L[3] *= 2, 避免L[3]过小造成频繁插入失败
        vacuumFilter.alternateRange.set(3, vacuumFilter.alternateRange.get(3) * 2);
        vacuumFilter.bucketNum = getBucketNum(vacuumFilter.maxSize, vacuumFilter.alternateRange);
        // 已经插入的元素数量
        vacuumFilter.size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 原始数据的字节长度
        vacuumFilter.itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // 指纹哈希密钥
        byte[] fingerprintHashKey = byteArrayList.remove(0);
        vacuumFilter.fingerprintHash = PrfFactory.createInstance(envType, FINGERPRINT_BYTE_LENGTH);
        vacuumFilter.fingerprintHash.setKey(fingerprintHashKey);
        // 桶哈希密钥
        byte[] bucketHashKey = byteArrayList.remove(0);
        vacuumFilter.bucketHash = PrfFactory.createInstance(envType, Integer.BYTES);
        vacuumFilter.bucketHash.setKey(bucketHashKey);
        vacuumFilter.secureRandom = new SecureRandom();
        // 桶中的元素
        Preconditions.checkArgument(byteArrayList.size() == vacuumFilter.bucketNum * ENTRIES_PER_BUCKET);
        ByteBuffer[] bucketFlattenedElements = byteArrayList.stream().map(ByteBuffer::wrap).toArray(ByteBuffer[]::new);
        vacuumFilter.buckets = IntStream.range(0, vacuumFilter.bucketNum)
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

        return vacuumFilter;
    }

    private VacuumFilter() {
        // empty
    }

    @Override
    public List<byte[]> toByteArrayList() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // 过滤器类型
        byteArrayList.add(IntUtils.intToByteArray(getFilterType().ordinal()));
        // 期望插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(maxSize));
        // 已经插入的元素数量
        byteArrayList.add(IntUtils.intToByteArray(size));
        // 原始数据的字节长度
        byteArrayList.add(IntUtils.intToByteArray(itemByteLength));
        // 指纹哈希密钥
        byteArrayList.add(BytesUtils.clone(fingerprintHash.getKey()));
        // 桶哈希密钥
        byteArrayList.add(BytesUtils.clone(bucketHash.getKey()));
        // 桶中的元素
        IntStream.range(0, bucketNum).forEach(bucketIndex -> {
            // 插入元素内容，空元素用0占位
            List<CuckooFilterEntry> bucket = buckets.get(bucketIndex);
            int remainSize = ENTRIES_PER_BUCKET - bucket.size();
            for (CuckooFilterEntry cuckooFilterEntry : bucket) {
                byte[] fingerprintBytes = cuckooFilterEntry.getFingerprint().array();
                byteArrayList.add(BytesUtils.clone(fingerprintBytes));
            }
            while (remainSize > 0) {
                byteArrayList.add(new byte[0]);
                remainSize--;
            }
        });
        return byteArrayList;
    }

    @Override
    public boolean mightContain(Object data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        CuckooFilterEntry cuckooFilterEntry = new CuckooFilterEntry(fingerprint);
        return buckets.get(bucketIndex1).contains(cuckooFilterEntry)
            || buckets.get(bucketIndex2).contains(cuckooFilterEntry);
    }

    @Override
    public synchronized void put(Object data) {
        assert size < maxSize;
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.getInteger(objectBytes, bucketNum);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
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
            int choiceBucketIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            List<CuckooFilterEntry> choiceBucket = buckets.get(choiceBucketIndex);
            ByteBuffer choiceFingerprint;
            ByteBuffer addedFingerprint = fingerprint;
            int choiceBucketIndex2;
            int choiceEntryIndex;
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // 广度优先 + 深度优先
                for (CuckooFilterEntry entry : choiceBucket) {
                    choiceFingerprint = entry.getFingerprint();
                    choiceBucketIndex2 = alternativeIndex(choiceBucketIndex, choiceFingerprint);
                    if (buckets.get(choiceBucketIndex2).size() < ENTRIES_PER_BUCKET) {
                        buckets.get(choiceBucketIndex2).add(new CuckooFilterEntry(choiceFingerprint));
                        buckets.get(choiceBucketIndex).remove(new CuckooFilterEntry(choiceFingerprint));
                        buckets.get(choiceBucketIndex).add(new CuckooFilterEntry(addedFingerprint));
                        size++;
                        itemByteLength += objectBytes.length;
                        return;
                    }
                }
                choiceEntryIndex = secureRandom.nextInt(ENTRIES_PER_BUCKET);
                choiceFingerprint = choiceBucket.remove(choiceEntryIndex).getFingerprint();
                choiceBucket.add(new CuckooFilterEntry(addedFingerprint));
                assert choiceBucket.size() == ENTRIES_PER_BUCKET;
                addedFingerprint = choiceFingerprint;
                choiceBucketIndex = alternativeIndex(choiceBucketIndex, addedFingerprint);
                choiceBucket = buckets.get(choiceBucketIndex);
            }
            // 如果到达这个位置，意味着不能再踢出元素了
            throw new IllegalArgumentException("Cannot add item, exceeding max tries: " + data);
        }
    }

    /**
     * 得到候选的index。
     *
     * @param bucketIndex index。
     * @param fingerprint 指纹值。
     */
    private int alternativeIndex(int bucketIndex, ByteBuffer fingerprint) {
        // 论文中提到小集合时（小于2^18)不需要分四层
        if (maxSize >= 1 << 18) {
            // 取指纹值的正数
            BigInteger fingerprintBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(fingerprint.array());
            int l = alternateRange.get(fingerprintBigInteger.mod(BigInteger.valueOf(ALTERNATE_RANGE_NUM)).intValue());
            int delta = bucketHash.getInteger(fingerprint.array(), bucketNum) % l;
            return bucketIndex ^ delta;
        } else {
            int delta = bucketHash.getInteger(fingerprint.array(), bucketNum) % bucketNum;
            int bPrime = (bucketIndex - delta) % bucketNum;
            return (bucketNum - 1 - bPrime + delta) % bucketNum;
        }
    }

    /**
     * 计算最佳的输出index范围，从L = 1开始进行执行loadFactorTest()进行测试，不通过则扩大L为两倍，直到通过测试为止。
     *
     * @param n 待插入的元素数量。
     * @param r 元素比例。
     */
    private static int rangeSelection(int n, double r) {
        int l = 1;
        while (!loadFactorTest(n, r, l)) {
            l *= 2;
        }
        return l;
    }

    /**
     * 测试输出范围L是否可以满足条件。
     *
     * @param n        待插入的元素数量。
     * @param r        元素比例。
     * @param capitalL 输出范围。
     */
    private static boolean loadFactorTest(int n, double r, int capitalL) {
        int m = DoubleMath.roundToInt(n / (4 * VacuumFilter.LOAD_FACTOR * capitalL), RoundingMode.UP) * capitalL;
        int capitalN = DoubleMath.roundToInt(4 * r * m * VacuumFilter.LOAD_FACTOR, RoundingMode.UP);
        int c = DoubleMath.roundToInt((double)m / capitalL, RoundingMode.UP);
        int p = DoubleMath.roundToInt(0.97 * 4 * capitalL, RoundingMode.UP);
        int d = DoubleMath.roundToInt((double)capitalN / c + 1.5 * Math.sqrt((double)2 * capitalN / c * Math.log(c)),
            RoundingMode.UP);
        return d < p;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.VACUUM_FILTER;
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
    public double ratio() {
        int vacuumFilterByteLength = size * FINGERPRINT_BYTE_LENGTH +
            (bucketNum * ENTRIES_PER_BUCKET - size) * CommonUtils.getByteLength(1);
        return ((double)vacuumFilterByteLength) / itemByteLength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VacuumFilter)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        //noinspection unchecked
        VacuumFilter<T> that = (VacuumFilter<T>)obj;
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
            .append(bucketHash.getPrfType())
            .append(bucketHash.getKey());
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new HashSet<>(buckets.get(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

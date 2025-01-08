package edu.alibaba.mpc4j.common.structure.rcfilter;

import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract random vacuum filter.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
abstract class AbstractRandomVacuumFilter implements RandomCuckooFilter {
    /**
     * number of entries in each bucket, should be 2^k. Vacuum Filter must set 4 since it uses the same parameter to
     * compute alternate ranges.
     */
    static final int ENTRIES_PER_BUCKET = 4;
    /**
     * alternative range num, should be the same as number of entries in each bucket. The default value is 4.
     */
    static final int ALTERNATE_RANGE_NUM = 4;
    /**
     * max number of kicks for collusion, we se 2^11 = 2048 based on experiments.
     */
    private static final int MAX_NUM_KICKS = 1 << 11;

    /**
     * Gets bucket num.
     *
     * @param maxSize    max size.
     * @param loadFactor load factor α.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize, double loadFactor) {
        // See Section 3.4, L0 = RangeSelection(n, α, 1)
        int l0 = rangeSelection(maxSize, loadFactor, 1.0);
        // we can simply calculate m = n / (1 / α) / 4 and round up
        int m = DoubleMath.roundToInt((1.0 / loadFactor) * maxSize / ENTRIES_PER_BUCKET, RoundingMode.UP);
        // See Section 3.3, m is a multiple of L, so we also need to round up.
        return (m / l0 + 1) * l0;
    }

    /**
     * Gets bucket num.
     *
     * @param maxSize        max size.
     * @param loadFactor     load factor α.
     * @param alternateRange pre-computed alternate range.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize, double loadFactor, int[] alternateRange) {
        assert alternateRange.length == ALTERNATE_RANGE_NUM;
        // we can simply calculate m = n / (1 / α) / 4 and round up
        int n = DoubleMath.roundToInt((1.0 / loadFactor) * maxSize / ALTERNATE_RANGE_NUM, RoundingMode.UP);
        // See Section 3.3, m is a multiple of L, so we also need to round up, AR[0] is L0 = RangeSelection(n, α, 1)
        return (n / alternateRange[0] + 1) * alternateRange[0];
    }

    /**
     * Sets alternate range.
     *
     * @param maxSize        max size.
     * @param loadFactor     load factor α.
     * @param alternateRange initial alternate range.
     */
    static void setAlternateRange(int maxSize, double loadFactor, int[] alternateRange) {
        assert alternateRange.length == ALTERNATE_RANGE_NUM;
        // See Algorithm 3: for i = 0; i < 4; i++ do L[i] = RangeSelection(n, 0.95, 1 − i/4).
        for (int i = 0; i < ALTERNATE_RANGE_NUM; i++) {
            alternateRange[i] = rangeSelection(maxSize, loadFactor, (4 - i) * 0.25);
        }
        // L[3] *= 2, enlarge L[3] to avoid failures.
        alternateRange[3] = alternateRange[3] * 2;
    }

    /**
     * RangeSelection(n, α, r) shown in Algorithm 2 of the paper. RangeSelection is used to select the minimum
     * alternative range size L that can pass the load factor test to achieve good locality.
     *
     * @param n          the number of items n.
     * @param loadFactor load factor α.
     * @param r          a parameter that shows the ratio of inserted items in the total number of items.
     * @return the minimum alternative range size L that can pass the load factor test to achieve good locality.
     */
    static int rangeSelection(int n, double loadFactor, double r) {
        int l = 1;
        while (!loadFactorTest(n, loadFactor, r, l)) {
            l *= 2;
        }
        return l;
    }

    /**
     * LoadFactorTest(n, α, r, L) shown in Algorithm 1 of the paper. LoadFactorTest is to test whether a specific
     * alternate range, L, can achieve the target load factor α given the number of items n.
     *
     * @param n          the number of items n.
     * @param loadFactor load factor α.
     * @param r          a parameter that shows the ratio of inserted items in the total number of items.
     * @param capitalL   a specific alternate range.
     * @return true if the alternate L can achieve the target load factor α given the number of items n; false otherwise.
     */
    static boolean loadFactorTest(int n, double loadFactor, double r, int capitalL) {
        // m = ⌈n / (4αL)⌉ * L, the number of buckets.
        int m = DoubleMath.roundToInt(n / (4 * loadFactor * capitalL), RoundingMode.UP) * capitalL;
        // N = 4 * r * m * α, the number of inserted items.
        int capitalN = DoubleMath.roundToInt(4 * r * m * loadFactor, RoundingMode.UP);
        // c = m / L, the number of chunks.
        int c = DoubleMath.roundToInt((double) m / capitalL, RoundingMode.UP);
        // P = 0.97 × 4L, the capacity lower bound of each chunk.
        int p = DoubleMath.roundToInt(0.97 * 4 * capitalL, RoundingMode.UP);
        // D = EstimatedMaxLoad(N, c) = n / c + 3 / 2 * √(2n / c · log(c))
        int d = DoubleMath.roundToInt(
            (double) capitalN / c + 1.5 * Math.sqrt((double) 2 * capitalN / c * Math.log(c)), RoundingMode.UP
        );
        // if D < P then return Pass; else return Fail.
        return d < p;
    }

    /**
     * byte length for each fingerprint
     */
    protected final int fingerprintByteLength;
    /**
     * fingerprint bit length
     */
    protected final int fingerprintBitLength;
    /**
     * α, number of elements in buckets / total number of elements.
     */
    protected final double loadFactor;
    /**
     * empty fingerprint
     */
    protected final long emptyFingerprint;
    /**
     * max size, i.e., max number of inserted items.
     */
    protected final int maxSize;
    /**
     * bucket num, should be a multiple of L0, where L0 = RangeSelection(n, α, 1).
     */
    protected final int bucketNum;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * alternate range, we set the number of alternate ranges as a power of two, so we can assign alternate ranges to
     * the items by their least significant bits. To balance both the load factor and locality, we use 4 different
     * alternate ranges in the final design of vacuum filters.
     */
    private final int[] alternateRange;
    /**
     * buckets
     */
    protected final ArrayList<TLongArrayList> buckets;
    /**
     * size, i.e., number of inserted items.
     */
    protected int size;

    protected AbstractRandomVacuumFilter(int maxSize, int fingerprintByteLength, double loadFactor) {
        // set constant parameters
        this.fingerprintByteLength = fingerprintByteLength;
        fingerprintBitLength = fingerprintByteLength * Byte.SIZE;
        emptyFingerprint = 0L;
        this.loadFactor = loadFactor;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        alternateRange = new int[ALTERNATE_RANGE_NUM];
        setAlternateRange(maxSize, loadFactor, alternateRange);
        bucketNum = getBucketNum(maxSize,loadFactor, alternateRange);
        secureRandom = new SecureRandom();
        // set buckets
        buckets = IntStream.range(0, bucketNum)
            .mapToObj(bucketIndex -> new TLongArrayList(ENTRIES_PER_BUCKET))
            .collect(Collectors.toCollection(ArrayList::new));
        size = 0;
    }

    private int positionHash(long ele) {
        // return (ele % n + n) % n
        return ((int) ele % bucketNum + bucketNum) % bucketNum;
    }

    /**
     * Computes the alternative bucket index by giving the current bucket index and the fingerprint.
     * <p></p>
     * Section 3.3 of the paper defines both bucket indexes. In details, Vacuum filter divide the whole table into
     * multiple equal-size chunks, each of which includes $L$ consecutive buckets and $L$ is a power of two. Hence $m$
     * is a multiple of $L$, instead of being restricted to a power of two as in cuckoo filters. The two candidate
     * buckets of each item should be in the same chunk. For each item $x$, we compute the indices of the two alternate
     * buckets using
     * <ul>
     * <li>B_1(x) = H(x) mod m</li>
     * <li>B_2(x) = Alt(B_1(x), f) = B_1(x) ⊕ (H'(f) mod L)</li>
     * </ul>
     * We call the length of chunk, $L$, as the alternate range (AR).
     *
     * @param bucketIndex current bucket index.
     * @param fingerprint fingerprint.
     * @return alternative bucket index.
     */
    private int alternativeIndex(int bucketIndex, long fingerprint) {
        if (maxSize >= 1 << 18) {
            // l = L[f mod 4], Current alternate range, we use last 2 bits in the fingerprint to decide l.
            int fingerprintPosition = (int) fingerprint & 0xFF;
            assert IntMath.isPowerOfTwo(ALTERNATE_RANGE_NUM);
            int l = alternateRange[fingerprintPosition & (ALTERNATE_RANGE_NUM - 1)];
            // Δ = H'(f) mod l
            int delta = positionHash(fingerprint) % l;
            // return B ⊕ Δ
            return bucketIndex ^ delta;
        } else {
            // When the number of keys is smaller than 2^18, use Algorithm 4 of the paper to decide alternate ranges.
            // here B is the current bucket index, and f is the fingerprint.
            // Δ' = H'(f) mod m
            int delta = positionHash(fingerprint) % bucketNum;
            // B' = (B − Δ') mod m
            int bPrime = (bucketIndex - delta) % bucketNum;
            // return (m - 1 - B' + Δ') mod m
            return (bucketNum - 1 - bPrime + delta) % bucketNum;
        }
    }

    private long computeFingerprint(long ele) {
        return Math.abs(RandomCuckooFilterHashUtils.murmurHash64(ele ^ 0x192837319273L) % ((1L << fingerprintBitLength) - 1)) + 1;
    }

    @Override
    public boolean mightContain(long data) {
        long ele = RandomCuckooFilterHashUtils.murmurHash64(data ^ 0x12891927L);
        long fingerprint = computeFingerprint(ele);
        int bucketIndex1 = positionHash(ele);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        return buckets.get(bucketIndex1).contains(fingerprint) || buckets.get(bucketIndex2).contains(fingerprint);
    }

    @Override
    public TIntSet modifyPut(long data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        TIntHashSet intHashSet = new TIntHashSet();
        // B_1 = H(x), B_2 = Alt(B_1, f).
        long ele = RandomCuckooFilterHashUtils.murmurHash64(data ^ 0x12891927L);
        long fingerprint = computeFingerprint(ele);
        int bucketIndex1 = positionHash(ele);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        // if B_1 or B_2 has an empty slot, then put f into the empty slot and return Success.
        if (buckets.get(bucketIndex1).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex1).add(fingerprint);
            intHashSet.add(bucketIndex1);
            size++;
            return intHashSet;
        } else if (buckets.get(bucketIndex2).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex2).add(fingerprint);
            intHashSet.add(bucketIndex2);
            size++;
            return intHashSet;
        } else {
            // Randomly select a bucket B from B_1 and B_2.
            int choiceBucketIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            TLongArrayList choiceBucket = buckets.get(choiceBucketIndex);
            long ejectFingerprint;
            long addedFingerprint = fingerprint;
            int ejectBucketIndex;
            int choiceEntryIndex;
            // for i = 0; i < MaxEvicts; i++ do
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // Extend Search Scope, foreach fingerprint f' in B do
                for (long entry : choiceBucket.toArray()) {
                    ejectFingerprint = entry;
                    ejectBucketIndex = alternativeIndex(choiceBucketIndex, ejectFingerprint);
                    // if Bucket Alt(B, f') has an empty slot
                    if (buckets.get(ejectBucketIndex).size() < ENTRIES_PER_BUCKET) {
                        // put f' to the empty slot
                        buckets.get(ejectBucketIndex).add(ejectFingerprint);
                        buckets.get(choiceBucketIndex).remove(ejectFingerprint);
                        // put f to the original slot of f'
                        buckets.get(choiceBucketIndex).add(addedFingerprint);
                        intHashSet.add(choiceBucketIndex);
                        intHashSet.add(ejectBucketIndex);
                        size++;
                        return intHashSet;
                    }
                }
                // Randomly select a slot s from bucket B
                choiceEntryIndex = secureRandom.nextInt(ENTRIES_PER_BUCKET);
                // Swap f and the fingerprint stored in the slot s
                ejectFingerprint = choiceBucket.removeAt(choiceEntryIndex);
                choiceBucket.add(addedFingerprint);
                assert choiceBucket.size() == ENTRIES_PER_BUCKET;
                intHashSet.add(choiceBucketIndex);
                addedFingerprint = ejectFingerprint;
                // B = Alt(B, f)
                choiceBucketIndex = alternativeIndex(choiceBucketIndex, addedFingerprint);
                choiceBucket = buckets.get(choiceBucketIndex);
            }
            // reaching here means we cannot successfully add fingerprint
            throw new IllegalArgumentException("Cannot add item, exceeding max tries: " + data);
        }
    }

    @Override
    public int modifyRemove(long data) {
        long ele = RandomCuckooFilterHashUtils.murmurHash64(data ^ 0x12891927L);
        long fingerprint = computeFingerprint(ele);
        int bucketIndex1 = positionHash(ele);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        boolean removeBucketIndex1 = buckets.get(bucketIndex1).remove(fingerprint);
        boolean removeBucketIndex2 = buckets.get(bucketIndex2).remove(fingerprint);
        if ((!removeBucketIndex1) && (!removeBucketIndex2)) {
            // both buckets do not contain data
            throw new IllegalArgumentException("Remove a not-contained item: " + data);
        } else if (removeBucketIndex1 && removeBucketIndex2) {
            // remove same data from both buckets
            throw new IllegalArgumentException("Remove the contained item from both buckets: " + data);
        } else if (removeBucketIndex1) {
            size--;
            return bucketIndex1;
        } else {
            size--;
            return bucketIndex2;
        }
    }

    @Override
    public TLongArrayList getBucket(int index) {
        return buckets.get(index);
    }

    @Override
    public int getEntriesPerBucket() {
        return ENTRIES_PER_BUCKET;
    }

    @Override
    public int getFingerprintByteLength() {
        return fingerprintByteLength;
    }

    @Override
    public int getBucketNum() {
        return bucketNum;
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
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractRandomVacuumFilter that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size);
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            equalsBuilder.append(
                new TLongHashSet(this.buckets.get(buckedIndex)),
                new TLongHashSet(that.buckets.get(buckedIndex))
            )
        );
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder
            .append(maxSize)
            .append(size);
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new TLongHashSet(buckets.get(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

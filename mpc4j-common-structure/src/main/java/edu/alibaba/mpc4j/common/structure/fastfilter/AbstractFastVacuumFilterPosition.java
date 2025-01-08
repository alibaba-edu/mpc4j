package edu.alibaba.mpc4j.common.structure.fastfilter;

import com.google.common.math.DoubleMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.LongHash;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.math.RoundingMode;

/**
 * abstract fast vacuum filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractFastVacuumFilterPosition<T> implements FastCuckooFilterPosition<T> {
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
        int d = (c == 1) ? capitalN : DoubleMath.roundToInt(
            (double) capitalN / c + 1.5 * Math.sqrt((double) 2 * capitalN / c * Math.log(c)), RoundingMode.UP
        );
        // if D < P then return Pass; else return Fail.
        return d < p;
    }

    /**
     * bit length for each fingerprint
     */
    protected final int fingerprintBitLength;
    /**
     * byte length for each fingerprint
     */
    protected final int fingerprintByteLength;
    /**
     * tag mask
     */
    private final long fingerprintMask;
    /**
     * α, number of elements in buckets / total number of elements.
     */
    protected final double loadFactor;
    /**
     * max number of elements.
     */
    protected final int maxSize;
    /**
     * bucket num
     */
    protected final int bucketNum;
    /**
     * hash for mapping data to long.
     */
    protected final LongHash hash;
    /**
     * hash seed
     */
    protected final long hashSeed;
    /**
     * alternate range, we set the number of alternate ranges as a power of two, so we can assign alternate ranges to
     * the items by their least significant bits. To balance both the load factor and locality, we use 4 different
     * alternate ranges in the final design of vacuum filters.
     */
    private final int[] alternateRange;

    public AbstractFastVacuumFilterPosition(int maxSize, long hashSeed, int fingerprintBitLength, double loadFactor) {
        assert fingerprintBitLength > 0 && fingerprintBitLength <= Long.SIZE;
        this.fingerprintBitLength = fingerprintBitLength;
        fingerprintByteLength = CommonUtils.getByteLength(fingerprintBitLength);
        if (fingerprintBitLength == Long.SIZE) {
            fingerprintMask = 0xFFFFFFFF_FFFFFFFFL;
        } else if (fingerprintBitLength == Long.SIZE - 1) {
            fingerprintMask = 0x7FFFFFFF_FFFFFFFFL;
        } else {
            fingerprintMask = (1L << fingerprintBitLength) - 1;
        }
        this.loadFactor = loadFactor;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        alternateRange = new int[ALTERNATE_RANGE_NUM];
        setAlternateRange(maxSize, loadFactor, alternateRange);
        bucketNum = getBucketNum(maxSize, loadFactor, alternateRange);
        hash = LongHashFactory.fastestInstance();
        this.hashSeed = hashSeed;
    }

    protected int positionHash(long ele) {
        return ((int) ele % bucketNum + bucketNum) % bucketNum;
    }

    protected long tagHash(long ele) {
        long tag = murmurHash64(ele ^ 0x192837319273L) & fingerprintMask;
        if (tag == 0) {
            tag++;
        }
        return tag;
    }

    private long murmurHash64(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    /**
     * Gets other bucket index.
     *
     * @param index 1st bucket index.
     * @param tag   tag.
     * @return 2nd bucket index.
     */
    protected int altIndex(int index, long tag) {
        if (maxSize >= 1 << 18) {
            // l = L[f mod 4], Current alternate range, we use last 2 bits in the fingerprint to decide l.
            int l = alternateRange[(int) tag & 0b11];
            // Δ = H'(f) mod l
            int delta = (int) tag * 0x5bd1e995;
            delta &= (l - 1);
            // return B ⊕ Δ
            return index ^ delta;
        } else {
            // When the number of keys is smaller than 2^18, use Algorithm 4 of the paper to decide alternate ranges.
            // here B is the current bucket index, and f is the fingerprint.
            // Δ' = H'(f) mod m
            int delta = (((int) tag * 0x5bd1e995) % bucketNum + bucketNum) % bucketNum;
            // B' = (B − Δ') mod m
            int bPrime = (index - delta) % bucketNum;
            // return (m - 1 - B' + Δ') mod m
            return (bucketNum - 1 - bPrime + delta) % bucketNum;
        }
    }

    @Override
    public int[] positions(T data) {
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        long fingerprint = tagHash(ele);
        int[] positions = new int[2];
        // B_1 = H(x), B_2 = Alt(B_1, f).
        positions[0] = positionHash(ele);
        positions[1] = altIndex(positions[0], fingerprint);
        assert positions[0] == altIndex(positions[1], fingerprint);
        return positions;
    }

    @Override
    public long fingerprint(T data) {
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        return tagHash(ele);
    }

    @Override
    public int maxSize() {
        return maxSize;
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
    public int getFingerprintBitLength() {
        return fingerprintBitLength;
    }

    @Override
    public int getBucketNum() {
        return bucketNum;
    }
}

package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.math.RoundingMode;
import java.nio.ByteBuffer;

/**
 * abstract vacuum filter position
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractVacuumFilterPosition<T> implements CuckooFilterPosition<T> {
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
     * byte length for each fingerprint
     */
    protected final int fingerprintByteLength;
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
     * bucket hash
     */
    protected final IntHash bucketHash;
    /**
     * bucket hash key
     */
    protected final byte[] bucketHashKey;
    /**
     * bucket hash seed
     */
    protected final int bucketHashSeed;
    /**
     * fingerprint hash
     */
    protected final Prf fingerprintHash;
    /**
     * alternate range, we set the number of alternate ranges as a power of two, so we can assign alternate ranges to
     * the items by their least significant bits. To balance both the load factor and locality, we use 4 different
     * alternate ranges in the final design of vacuum filters.
     */
    private final int[] alternateRange;

    public AbstractVacuumFilterPosition(EnvType envType, int maxSize, byte[][] keys,
                                        int fingerprintByteLength, double loadFactor) {
        this.fingerprintByteLength = fingerprintByteLength;
        this.loadFactor = loadFactor;
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        alternateRange = new int[ALTERNATE_RANGE_NUM];
        setAlternateRange(maxSize, loadFactor, alternateRange);
        bucketNum = getBucketNum(maxSize, loadFactor, alternateRange);
        // set hashes
        assert keys.length == CuckooFilter.getHashKeyNum();
        fingerprintHash = PrfFactory.createInstance(envType, fingerprintByteLength);
        fingerprintHash.setKey(keys[0]);
        bucketHash = IntHashFactory.fastestInstance();
        bucketHashKey = BytesUtils.clone(keys[1]);
        bucketHashSeed = ByteBuffer.wrap(keys[1]).getInt();
    }

    @Override
    public int[] positions(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int[] positions = new int[2];
        // B_1 = H(x), B_2 = Alt(B_1, f).
        positions[0] = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        positions[1] = alternativeIndex(positions[0], fingerprint);
        return positions;
    }

    @Override
    public ByteBuffer fingerprint(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        return ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
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
    protected int alternativeIndex(int bucketIndex, ByteBuffer fingerprint) {
        if (maxSize >= 1 << 18) {
            // l = L[f mod 4], Current alternate range, we use last 2 bits in the fingerprint to decide l.
            byte fingerprintByte = fingerprint.array()[0];
            assert IntMath.isPowerOfTwo(NaiveVacuumFilter.ALTERNATE_RANGE_NUM);
            int l = alternateRange[fingerprintByte & (NaiveVacuumFilter.ALTERNATE_RANGE_NUM - 1)];
            // Δ = H'(f) mod l
            int delta = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum) % l;
            // return B ⊕ Δ
            return bucketIndex ^ delta;
        } else {
            // When the number of keys is smaller than 2^18, use Algorithm 4 of the paper to decide alternate ranges.
            // here B is the current bucket index, and f is the fingerprint.
            // Δ' = H'(f) mod m
            int delta = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum) % bucketNum;
            // B' = (B − Δ') mod m
            int bPrime = (bucketIndex - delta) % bucketNum;
            // return (m - 1 - B' + Δ') mod m
            return (bucketNum - 1 - bPrime + delta) % bucketNum;
        }
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
    public int getBucketNum() {
        return bucketNum;
    }
}

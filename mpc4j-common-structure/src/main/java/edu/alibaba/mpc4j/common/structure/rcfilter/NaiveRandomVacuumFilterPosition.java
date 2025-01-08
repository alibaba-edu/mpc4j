package edu.alibaba.mpc4j.common.structure.rcfilter;

import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * naive vacuum filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
class NaiveRandomVacuumFilterPosition implements RandomCuckooFilterPosition {
    /**
     * fingerprint bit length
     */
    private final int fingerprintBitLength;
    /**
     * max number of elements.
     */
    private final int maxSize;
    /**
     * bucket num
     */
    private final int bucketNum;
    /**
     * alternate range, we set the number of alternate ranges as a power of two, so we can assign alternate ranges to
     * the items by their least significant bits. To balance both the load factor and locality, we use 4 different
     * alternate ranges in the final design of vacuum filters.
     */
    private final int[] alternateRange;

    public NaiveRandomVacuumFilterPosition(int maxSize) {
        MathPreconditions.checkPositive("maxSize", maxSize);
        fingerprintBitLength = NaiveRandomVacuumFilter.FINGERPRINT_BYTE_LENGTH * Byte.SIZE;
        this.maxSize = maxSize;
        alternateRange = new int[NaiveRandomVacuumFilter.ALTERNATE_RANGE_NUM];
        NaiveRandomVacuumFilter.setAlternateRange(maxSize, alternateRange);
        bucketNum = NaiveRandomVacuumFilter.getBucketNum(maxSize, alternateRange);
    }

    @Override
    public RandomCuckooFilterType getType() {
        return RandomCuckooFilterType.NAIVE_VACUUM_FILTER;
    }

    @Override
    public int[] positions(long data) {
        long ele = RandomCuckooFilterHashUtils.murmurHash64(data ^ 0x12891927L);
        long fingerprint = computeFingerprint(ele);
        int[] positions = new int[2];
        // B_1 = H(x), B_2 = Alt(B_1, f).
        positions[0] = positionHash(ele);
        positions[1] = alternativeIndex(positions[0], fingerprint);
        return positions;
    }

    @Override
    public long fingerprint(long data) {
        long ele = RandomCuckooFilterHashUtils.murmurHash64(data ^ 0x12891927L);
        return computeFingerprint(ele);
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
            assert IntMath.isPowerOfTwo(NaiveRandomVacuumFilter.ALTERNATE_RANGE_NUM);
            int l = alternateRange[fingerprintPosition & (NaiveRandomVacuumFilter.ALTERNATE_RANGE_NUM - 1)];
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
    public int maxSize() {
        return maxSize;
    }

    @Override
    public int getEntriesPerBucket() {
        return NaiveRandomVacuumFilter.ENTRIES_PER_BUCKET;
    }

    @Override
    public int getFingerprintByteLength() {
        return NaiveRandomVacuumFilter.FINGERPRINT_BYTE_LENGTH;
    }

    @Override
    public int getBucketNum() {
        return bucketNum;
    }
}

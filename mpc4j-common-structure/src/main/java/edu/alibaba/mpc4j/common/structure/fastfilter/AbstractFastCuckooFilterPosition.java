package edu.alibaba.mpc4j.common.structure.fastfilter;

import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.LongHash;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * abstract fast cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractFastCuckooFilterPosition<T> implements FastCuckooFilterPosition<T> {
    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize       max number of elements.
     * @param tagsPerBucket number of tags in each bucket.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize, int tagsPerBucket) {
        int bucketNum = IntMath.floorPowerOfTwo(Math.max(1, maxSize / tagsPerBucket));
        double loadFactor = (double) maxSize / bucketNum / tagsPerBucket;
        if (loadFactor > 0.96) {
            bucketNum <<= 1;
        }
        return bucketNum;
    }

    /**
     * number of tags in each bucket
     */
    protected final int entriesPerBucket;
    /**
     * tag bit length
     */
    protected final int fingerprintBitLength;
    /**
     * tag byte length
     */
    protected final int fingerprintByteLength;
    /**
     * tag mask
     */
    private final long fingerprintMask;
    /**
     * max number of elements
     */
    protected final int maxSize;
    /**
     * number of buckets
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

    public AbstractFastCuckooFilterPosition(int maxSize, long hashSeed, int entriesPerBucket, int fingerprintBitLength) {
        // assign parameters that are not assigned by users.
        assert entriesPerBucket > 0;
        this.entriesPerBucket = entriesPerBucket;
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
        // assign parameters that are assigned by users.
        MathPreconditions.checkPositive("maxNum", maxSize);
        this.maxSize = maxSize;
        bucketNum = getBucketNum(maxSize, entriesPerBucket);
        assert IntMath.isPowerOfTwo(bucketNum);
        hash = LongHashFactory.fastestInstance();
        this.hashSeed = hashSeed;
    }

    /**
     * Gets bucket index of the given hashed value.
     *
     * @param hv hashed value.
     * @return bucket index.
     */
    protected int indexHash(long hv) {
        // hashed value is random, bucketNum is always a power of two, we directly use lower bits as the bucket index.
        return (int) hv & (bucketNum - 1);
    }

    /**
     * Gets tag of the given hashed value.
     *
     * @param hv hashed value.
     * @return tag.
     */
    protected long tagHash(long hv) {
        long tag = (hv & fingerprintMask);
        // we avoid tag = 0
        if (tag == 0) {
            tag += 1;
        }
        return tag;
    }

    /**
     * Gets 1st bucket index.
     *
     * @param hv hashed value.
     * @return 1st bucket index.
     */
    protected int headIndex(long hv) {
        // index = IndexHash(hash >> 32);
        return indexHash(hv >>> (Long.SIZE - Integer.SIZE));
    }

    /**
     * Gets other bucket index.
     *
     * @param index 1st bucket index.
     * @param tag   tag.
     * @return 2nd bucket index.
     */
    protected int altIndex(int index, long tag) {
        // 0x5bd1e995 is the hash constant from MurmurHash2
        return indexHash(index ^ (tag * 0x5bd1e995L));
    }


    @Override
    public int[] positions(T data) {
        int[] positions = new int[2];
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        positions[0] = headIndex(hv);
        long tag = tagHash(hv);
        positions[1] = altIndex(positions[0], tag);
        return positions;
    }

    @Override
    public long fingerprint(T data) {
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        return tagHash(hv);
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public int getEntriesPerBucket() {
        return entriesPerBucket;
    }

    @Override
    public int getFingerprintBitLength() {
        return fingerprintBitLength;
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

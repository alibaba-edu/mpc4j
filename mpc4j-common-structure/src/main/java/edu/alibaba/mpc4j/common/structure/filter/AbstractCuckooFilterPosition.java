package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.math.DoubleMath;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.math.RoundingMode;
import java.nio.ByteBuffer;

/**
 * abstract cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractCuckooFilterPosition<T> implements CuckooFilterPosition<T> {
    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize    number of elements.
     * @param loadFactor load factor α..=
     * @return bucket num.
     */
    static int getBucketNum(int maxSize, double loadFactor, int entriesPerBucket) {
        return 1 << LongUtils.ceilLog2(
            DoubleMath.roundToInt((1.0 / loadFactor) * maxSize / entriesPerBucket, RoundingMode.UP) + 1
        );
    }

    /**
     * entries per bucket
     */
    protected final int entriesPerBucket;
    /**
     * fingerprint byte length
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

    public AbstractCuckooFilterPosition(EnvType envType, int maxSize, byte[][] keys,
                                        int entriesPerBucket, int fingerprintByteLength, double loadFactor) {
        this.entriesPerBucket = entriesPerBucket;
        this.fingerprintByteLength = fingerprintByteLength;
        this.loadFactor = loadFactor;
        // set dynamic parameters
        MathPreconditions.checkPositive("maxSize", maxSize);
        this.maxSize = maxSize;
        bucketNum = getBucketNum(maxSize, loadFactor, entriesPerBucket);
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
        // hash1 = Hash(data), hash = Hash(fingerprint), hash1 ^ hash2 = hash
        positions[0] = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int fingerPrintHash = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum);
        positions[1] = positions[0] ^ fingerPrintHash;
        return positions;
    }

    @Override
    public ByteBuffer fingerprint(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        return ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
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
    public int getFingerprintByteLength() {
        return fingerprintByteLength;
    }

    @Override
    public int getBucketNum() {
        return bucketNum;
    }
}

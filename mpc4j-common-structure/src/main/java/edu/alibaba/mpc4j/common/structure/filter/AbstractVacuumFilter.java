package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract vacuum filter.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
abstract class AbstractVacuumFilter<T> extends AbstractVacuumFilterPosition<T> implements CuckooFilter<T> {
    /**
     * max number of kicks for collusion, we se 2^12 = 4096 based on experiments.
     */
    private static final int MAX_NUM_KICKS = 1 << 12;
    /**
     * empty fingerprint
     */
    protected final ByteBuffer emptyFingerprint;
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * buckets
     */
    protected final ArrayList<ArrayList<ByteBuffer>> buckets;
    /**
     * size, i.e., number of inserted items.
     */
    protected int size;
    /**
     * item byte length
     */
    protected int itemByteLength;

    protected AbstractVacuumFilter(EnvType envType, int maxSize, byte[][] keys,
                                   int fingerprintByteLength, double loadFactor) {
        super(envType, maxSize, keys, fingerprintByteLength, loadFactor);
        emptyFingerprint = ByteBuffer.wrap(new byte[fingerprintByteLength]);
        secureRandom = new SecureRandom();
        // set buckets
        buckets = IntStream.range(0, bucketNum)
            .mapToObj(bucketIndex -> new ArrayList<ByteBuffer>(ENTRIES_PER_BUCKET))
            .collect(Collectors.toCollection(ArrayList::new));
        size = 0;
        itemByteLength = 0;
    }

    @Override
    public boolean mightContain(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        return buckets.get(bucketIndex1).contains(fingerprint) || buckets.get(bucketIndex2).contains(fingerprint);
    }

    @Override
    public TIntSet modifyPut(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        if (mightContain(data)) {
            throw new IllegalArgumentException("Insert might duplicate item: " + data);
        }
        TIntHashSet intHashSet = new TIntHashSet();
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        // B_1 = H(x), B_2 = Alt(B_1, f).
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int bucketIndex2 = alternativeIndex(bucketIndex1, fingerprint);
        // if B_1 or B_2 has an empty slot, then put f into the empty slot and return Success.
        if (buckets.get(bucketIndex1).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex1).add(fingerprint);
            intHashSet.add(bucketIndex1);
            size++;
            itemByteLength += objectBytes.length;
            return intHashSet;
        } else if (buckets.get(bucketIndex2).size() < ENTRIES_PER_BUCKET) {
            buckets.get(bucketIndex2).add(fingerprint);
            intHashSet.add(bucketIndex2);
            size++;
            itemByteLength += objectBytes.length;
            return intHashSet;
        } else {
            // Randomly select a bucket B from B_1 and B_2.
            int choiceBucketIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            List<ByteBuffer> choiceBucket = buckets.get(choiceBucketIndex);
            ByteBuffer ejectFingerprint;
            ByteBuffer addedFingerprint = fingerprint;
            int ejectBucketIndex;
            int choiceEntryIndex;
            // for i = 0; i < MaxEvicts; i++ do
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // Extend Search Scope, foreach fingerprint f' in B do
                for (ByteBuffer entry : choiceBucket) {
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
                        itemByteLength += objectBytes.length;
                        return intHashSet;
                    }
                }
                // Randomly select a slot s from bucket B
                choiceEntryIndex = secureRandom.nextInt(ENTRIES_PER_BUCKET);
                // Swap f and the fingerprint stored in the slot s
                ejectFingerprint = choiceBucket.remove(choiceEntryIndex);
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
    public int modifyRemove(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
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
    public ArrayList<ByteBuffer> getBucket(int index) {
        return buckets.get(index);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractVacuumFilter<?> that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.fingerprintHash.getPrfType(), that.fingerprintHash.getPrfType())
            .append(this.fingerprintHash.getKey(), that.fingerprintHash.getKey())
            .append(this.bucketHash.getType(), that.bucketHash.getType())
            .append(this.bucketHashKey, that.bucketHashKey);
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
            .append(bucketHash.getType())
            .append(bucketHashKey);
        // 因为插入顺序是没关系的，因此要变成集合
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new HashSet<>(buckets.get(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

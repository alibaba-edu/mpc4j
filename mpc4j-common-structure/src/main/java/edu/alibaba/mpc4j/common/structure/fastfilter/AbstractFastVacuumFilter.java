package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * abstract fast vacuum filter.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractFastVacuumFilter<T> extends AbstractFastVacuumFilterPosition<T> implements FastCuckooFilter<T> {
    /**
     * max number of kicks for collusion.
     */
    private static final int MAX_NUM_KICKS = 500;
    /**
     * buckets
     */
    protected final SingleTable table;
    /**
     * size, i.e., number of inserted items.
     */
    protected int size;
    /**
     * random state
     */
    private final Random random;

    protected AbstractFastVacuumFilter(int maxSize, long hashSeed, int fingerprintBitLength, double loadFactor) {
        super(maxSize, hashSeed, fingerprintBitLength, loadFactor);
        random = new Random();
        table = new SingleTable(fingerprintBitLength, ENTRIES_PER_BUCKET, bucketNum);
        size = 0;
    }

    @Override
    public boolean mightContain(T data) {
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        long fingerprint = tagHash(ele);
        int bucketIndex1 = positionHash(ele);
        int bucketIndex2 = altIndex(bucketIndex1, fingerprint);
        return table.findTagInBuckets(bucketIndex1, bucketIndex2, fingerprint);
    }

    @Override
    public void put(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        assert !mightContain(data) : "Insert might duplicate item: " + data;
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        long tag = tagHash(ele);
        int i1 = positionHash(ele);
        int i2 = altIndex(i1, tag);
        assert i1 == altIndex(i2, tag);
        addImpl(i1, i2, tag, null);
    }

    @Override
    public TIntSet modifyPut(T data) {
        MathPreconditions.checkLess("size", size, maxSize);
        assert !mightContain(data) : "Insert might duplicate item: " + data;
        TIntHashSet traceSet = new TIntHashSet();
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        long tag = tagHash(ele);
        int i1 = positionHash(ele);
        int i2 = altIndex(i1, tag);
        assert i1 == altIndex(i2, tag);
        addImpl(i1, i2, tag, traceSet);
        return traceSet;
    }

    private void addImpl(int i1, int i2, long tag, TIntHashSet traceSet) {
        assert traceSet == null || traceSet.isEmpty();
        // if B_1 or B_2 has an empty slot, then put f into the empty slot and return Success.
        if (table.insertTagToBucket(i1, tag)) {
            if (traceSet != null) {
                traceSet.add(i1);
            }
            size++;
        } else if (table.insertTagToBucket(i2, tag)) {
            if (traceSet != null) {
                traceSet.add(i2);
            }
            size++;
        } else {
            // Randomly select a bucket B from B_1 and B_2.
            int ib = random.nextBoolean() ? i1 : i2;
            long ejectFingerprint;
            long addedFingerprint = tag;
            int ejectBucketIndex;
            int choiceEntryIndex;
            // for i = 0; i < MaxEvicts; i++ do
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // Extend Search Scope, foreach fingerprint f' in B do
                for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                    ejectFingerprint = table.readTag(ib, j);
                    ejectBucketIndex = altIndex(ib, ejectFingerprint);
                    // if Bucket Alt(B, f') has an empty slot
                    if (table.insertTagToBucket(ejectBucketIndex, ejectFingerprint)) {
                        // successfully put f' to the empty slot
                        long deleteTag = table.deleteIndexFromBucket(ib, j);
                        assert deleteTag == ejectFingerprint;
                        // put f to the original slot of f'
                        boolean success = table.insertIndexToBucket(ib, j, addedFingerprint);
                        assert success;
                        if (traceSet != null) {
                            traceSet.add(ib);
                            traceSet.add(ejectBucketIndex);
                        }
                        size++;
                        return;
                    }
                }
                // Randomly select a slot s from bucket B
                choiceEntryIndex = random.nextInt(ENTRIES_PER_BUCKET);
                // Swap f and the fingerprint stored in the slot s
                ejectFingerprint = table.deleteIndexFromBucket(ib, choiceEntryIndex);
                boolean success = table.insertIndexToBucket(ib, choiceEntryIndex, addedFingerprint);
                assert success && table.numTagsInBucket(ib) == ENTRIES_PER_BUCKET;
                if (traceSet != null) {
                    traceSet.add(ib);
                }
                addedFingerprint = ejectFingerprint;
                // B = Alt(B, f)
                ib = altIndex(ib, addedFingerprint);
            }
            // reaching here means we cannot successfully add fingerprint
            throw new IllegalArgumentException("exceed max tries for adding " + size + "-th item, maxNum = " + maxSize);
        }
    }

    @Override
    public int modifyRemove(T data) {
        assert mightContain(data) : "Remove a not-contained item: " + data;
        long ele = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        int i1 = positionHash(ele);
        long tag = tagHash(ele);
        int i2 = altIndex(i1, tag);
        assert i1 == altIndex(i2, tag);
        // delete from the first bucket
        if (table.deleteTagFromBucket(i1, tag)) {
            size--;
            return i1;
        } else if (table.deleteTagFromBucket(i2, tag)) {
            size--;
            return i2;
        } else {
            throw new IllegalArgumentException("Remove a not-contained item: " + data);
        }
    }

    @Override
    public SingleTable getTable() {
        return table;
    }

    @Override
    public long[] getBucket(int index) {
        return table.getBucket(index);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractFastVacuumFilter<?> that)) {
            return false;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.hash.getType(), that.hash.getType())
            .append(this.hashSeed, that.hashSeed);
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            equalsBuilder.append(
                new TLongHashSet(this.table.getBucket(buckedIndex)),
                new TLongHashSet(that.table.getBucket(buckedIndex))
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
            .append(hash.getType())
            .append(hashSeed);
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            hashCodeBuilder.append(new TLongHashSet(table.getBucket(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.stream.IntStream;

/**
 * abstract fast cuckoo filter. The implementation is inspired by
 * <a href="https://github.com/efficient/cuckoofilter/blob/master/src/cuckoofilter.h">cuckoofilter.h</a>
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
abstract class AbstractFastCuckooFilter<T> extends AbstractFastCuckooFilterPosition<T> implements FastCuckooFilter<T> {
    /**
     * maximum number of cuckoo kicks before claiming failure.
     */
    private static final int K_MAX_CUCKOO_COUNT = 500;
    /**
     * table
     */
    protected final SingleTable table;
    /**
     * number of items
     */
    protected int size;

    public AbstractFastCuckooFilter(int maxSize, long hashSeed, int entriesPerBucket, int fingerprintBitLength) {
        super(maxSize, hashSeed, entriesPerBucket, fingerprintBitLength);
        table = new SingleTable(fingerprintBitLength, entriesPerBucket, bucketNum);
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean mightContain(T data) {
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        int i1 = headIndex(hv);
        long tag = tagHash(hv);
        int i2 = altIndex(i1, tag);
        assert (i1 == altIndex(i2, tag));
        return table.findTagInBuckets(i1, i2, tag);
    }

    public void put(T data) {
        MathPreconditions.checkLess("num", size, maxSize);
        assert !mightContain(data) : "Insert might duplicate item: " + data;
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        int i = headIndex(hv);
        long tag = tagHash(hv);
        addImpl(i, tag, null);
    }

    @Override
    public TIntSet modifyPut(T data) {
        MathPreconditions.checkLess("num", size, maxSize);
        assert !mightContain(data) : "Insert might duplicate item: " + data;
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        int i = headIndex(hv);
        long tag = tagHash(hv);
        TIntHashSet traceSet = new TIntHashSet();
        addImpl(i, tag, traceSet);
        return traceSet;
    }

    /**
     * Recursively add tag into table.
     *
     * @param i   bucket index.
     * @param tag tag.
     */
    private void addImpl(int i, long tag, TIntHashSet traceSet) {
        assert traceSet == null || traceSet.isEmpty();
        int currentIndex = i;
        long currentTag = tag;
        for (int count = 0; count < K_MAX_CUCKOO_COUNT; count++) {
            long oldTag = table.kickInsertTagToBucket(currentIndex, currentTag);
            if (traceSet != null) {
                traceSet.add(currentIndex);
            }
            if (oldTag != 0) {
                // this means we kick out an old tag
                currentTag = oldTag;
                currentIndex = altIndex(currentIndex, currentTag);
            } else {
                size++;
                return;
            }
        }
        // reaching here means we cannot successfully add fingerprint
        throw new IllegalArgumentException("exceed max tries for adding " + size + "-th item, maxNum = " + maxSize);
    }

    @Override
    public int modifyRemove(T data) {
        assert mightContain(data) : "Remove a not-contained item: " + data;
        long hv = hash.hash(ObjectUtils.objectToByteArray(data), hashSeed);
        int i1 = headIndex(hv);
        long tag = tagHash(hv);
        int i2 = altIndex(i1, tag);
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
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractFastCuckooFilter<?> that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder
            // compare constant parameters
            .append(this.entriesPerBucket, that.entriesPerBucket)
            .append(this.fingerprintByteLength, that.fingerprintByteLength)
            // compare dynamic parameters
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.hash.getType(), that.hash.getType())
            .append(this.hashSeed, that.hashSeed);
        // compare each bucket using set
        IntStream.range(0, bucketNum).forEach(buckedIndex ->
            equalsBuilder.append(
                new TLongHashSet(this.getBucket(buckedIndex)),
                new TLongHashSet(that.getBucket(buckedIndex))
            )
        );
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder
            // put constant parameters
            .append(entriesPerBucket)
            .append(fingerprintByteLength)
            // put dynamic parameters
            .append(maxSize)
            .append(size)
            .append(hash.getType())
            .append(hashSeed);
        // put bucket elements
        IntStream.range(0, bucketNum).forEach(
            buckedIndex -> hashCodeBuilder.append(new TLongHashSet(this.getBucket(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

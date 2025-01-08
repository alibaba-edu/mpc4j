package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;
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
 * abstract Cuckoo Filter. The scheme is described in the following paper:
 * <p>
 * Fan B, Andersen D G, Kaminsky M, et al. Cuckoo filter: Practically better than bloom. CoNET 2014, pp. 75-88.
 * </p>
 * Compared with Bloom Filter, Cuckoo Filter has the following advantages:
 * <ul>
 * <li>Support adding and removing items dynamically.</li>
 * <li>Provide higher lookup performance than traditional Bloom Filters.</li>
 * <li>Use less space than Bloom Filters in many practical applications.</li>
 * </ul>
 * The original Cuckoo Filter sets the number of elements in each bucket b = 4, tage size v = 42, and load factor = 0.955
 * to reach an FPP of ε_{max} = 2^{-40}. In the paper,
 * <p>
 * Kales, D., Rechberger, C., Schneider, T., Senker, M., Weinert, C.: Mobile private contact discovery at scale.
 * In: USENIX Security (2019)
 * </p>
 * the authors recommend setting b = 3, v = 32, load factor = 0.66 to reach an FPP of ε_{max} = 2^{-29}, and b = 3,
 * v = 42 to reach an FPP of ε_{max} = 2^{-39}. In the experiment, the authors only use the first set of parameters.
 * The paper also introduces a novel Cuckoo Filter Compression techniques. This abstract implementation contains core
 * algorithms for Cuckoo Filter. We separate parameter setting in the subclass instances.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
abstract class AbstractCuckooFilter<T> extends AbstractCuckooFilterPosition<T> implements CuckooFilter<T> {
    /**
     * max number of kicks for collusion. In paper, it is set to be 500. The test shows when inserting 2^20 elements,
     * there are some non-negligible failure probability. Here we set 2^10 = 1024, the same as cuckoo hash.
     */
    private static final int MAX_NUM_KICKS = 1 << 10;
    /**
     * empty fingerprint
     */
    protected final ByteBuffer emptyFingerprint;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;
    /**
     * cuckoo filter buckets
     */
    protected final ArrayList<ArrayList<ByteBuffer>> buckets;
    /**
     * number of inserted elements
     */
    protected int size;
    /**
     * item byte length, used for computing compress radio
     */
    protected int itemByteLength;

    protected AbstractCuckooFilter(EnvType envType, int maxSize, byte[][] keys,
                                   int entriesPerBucket, int fingerprintByteLength, double loadFactor) {
        super(envType, maxSize, keys, entriesPerBucket, fingerprintByteLength, loadFactor);
        emptyFingerprint = ByteBuffer.wrap(new byte[fingerprintByteLength]);
        secureRandom = new SecureRandom();
        // set buckets
        buckets = IntStream.range(0, bucketNum)
            .mapToObj(bucketIndex -> new ArrayList<ByteBuffer>(entriesPerBucket))
            .collect(Collectors.toCollection(ArrayList::new));
        size = 0;
        itemByteLength = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean mightContain(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        // hash1 = Hash(data), hash = Hash(fingerprint), hash1 ^ hash2 = hash
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int fingerprintHash = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum);
        int bucketIndex2 = bucketIndex1 ^ fingerprintHash;
        // check if one of the buckets contains fingerprint
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
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int fingerprintHash = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum);
        int bucketIndex2 = bucketIndex1 ^ fingerprintHash;
        // if bucket[i_1] or bucket[i_2] has an empty entry, then add f to that bucket
        if (buckets.get(bucketIndex1).size() < entriesPerBucket) {
            buckets.get(bucketIndex1).add(fingerprint);
            size++;
            itemByteLength += objectBytes.length;
            intHashSet.add(bucketIndex1);
            return intHashSet;
        } else if (buckets.get(bucketIndex2).size() < entriesPerBucket) {
            buckets.get(bucketIndex2).add(fingerprint);
            size++;
            itemByteLength += objectBytes.length;
            intHashSet.add(bucketIndex2);
            return intHashSet;
        } else {
            // i = randomly pick i_1 or i_2
            int choiceIndex = secureRandom.nextBoolean() ? bucketIndex1 : bucketIndex2;
            List<ByteBuffer> choiceBucket = buckets.get(choiceIndex);
            ByteBuffer addFingerprint = fingerprint;
            ByteBuffer ejectFingerprint;
            int choiceEntryIndex;
            for (int count = 0; count < MAX_NUM_KICKS; count++) {
                // randomly select an eject entry e from bucket[i]
                choiceEntryIndex = secureRandom.nextInt(entriesPerBucket);
                ejectFingerprint = choiceBucket.remove(choiceEntryIndex);
                // add the fingerprint into the ejected position
                choiceBucket.add(addFingerprint);
                intHashSet.add(choiceIndex);
                assert choiceBucket.size() == entriesPerBucket;
                addFingerprint = ejectFingerprint;
                int ejectFingerprintHash = bucketHash.hash(addFingerprint.array(), bucketHashSeed, bucketNum);
                choiceIndex = choiceIndex ^ ejectFingerprintHash;
                choiceBucket = buckets.get(choiceIndex);
                // bucket[i] has an empty entry, then add f to that bucket
                if (choiceBucket.size() < entriesPerBucket) {
                    choiceBucket.add(addFingerprint);
                    size++;
                    itemByteLength += objectBytes.length;
                    intHashSet.add(choiceIndex);
                    return intHashSet;
                }
                // reaching here means we need to iteratively eject elements
            }
            // reaching here means we cannot successfully add fingerprint
            throw new IllegalArgumentException("Cannot add item, exceeding max tries: " + data);
        }
    }

    @Override
    public int modifyRemove(T data) {
        byte[] objectBytes = ObjectUtils.objectToByteArray(data);
        ByteBuffer fingerprint = ByteBuffer.wrap(fingerprintHash.getBytes(objectBytes));
        // hash1 = Hash(data), hash = Hash(fingerprint), hash1 ^ hash2 = hash
        int bucketIndex1 = bucketHash.hash(objectBytes, bucketHashSeed, bucketNum);
        int fingerprintHash = bucketHash.hash(fingerprint.array(), bucketHashSeed, bucketNum);
        int bucketIndex2 = bucketIndex1 ^ fingerprintHash;
        // check if one of the buckets contains fingerprint, and remove it
        // note that if two bucket indexes are the same, then still only one remove returns true.
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
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractCuckooFilter<?> that)) {
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
            .append(this.loadFactor, that.loadFactor)
            // compare dynamic parameters
            .append(this.maxSize, that.maxSize)
            .append(this.size, that.size)
            .append(this.itemByteLength, that.itemByteLength)
            .append(this.fingerprintHash.getPrfType(), that.fingerprintHash.getPrfType())
            .append(this.fingerprintHash.getKey(), that.fingerprintHash.getKey())
            .append(this.bucketHash.getType(), that.bucketHash.getType())
            .append(this.bucketHashKey, that.bucketHashKey);
        // compare each bucket using set
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
            // put constant parameters
            .append(entriesPerBucket)
            .append(fingerprintByteLength)
            .append(loadFactor)
            // put dynamic parameters
            .append(maxSize)
            .append(size)
            .append(itemByteLength)
            .append(fingerprintHash.getPrfType())
            .append(fingerprintHash.getKey())
            .append(bucketHashKey);
        // put bucket elements
        IntStream.range(0, bucketNum).forEach(
            buckedIndex -> hashCodeBuilder.append(new HashSet<>(buckets.get(buckedIndex)))
        );
        return hashCodeBuilder.toHashCode();
    }
}

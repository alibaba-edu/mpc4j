package edu.alibaba.mpc4j.common.structure.rcfilter;

import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TIntSet;

import java.util.List;

/**
 * random cuckoo filter. It requires that inputs are random.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
public interface RandomCuckooFilter {
    /**
     * Gets random cuckoo filter type.
     *
     * @return random cuckoo filter type.
     */
    RandomCuckooFilterType getType();

    /**
     * Gets number of elements inserted into the random cuckoo filter.
     *
     * @return number of elements inserted into the random cuckoo filter.
     */
    int size();

    /**
     * Gets max number of elements that can be inserted into the random cuckoo filter.
     *
     * @return max number of elements that can be inserted into the random cuckoo filter.
     */
    int maxSize();

    /**
     * Tests if the random cuckoo filter might contain data.
     *
     * @param data data.
     * @return true if the random filter might contain data, false otherwise.
     */
    boolean mightContain(long data);

    /**
     * Gets the current byte size of the random cuckoo filter.
     *
     * @return current byte size of the random cuckoo filter.
     */
    default long byteSize() {
        List<byte[]> byteArrayList = save();
        return byteArrayList.stream()
            .mapToLong(element -> element.length)
            .sum();
    }

    /**
     * Packets the random cuckoo filter into {@code List<byte[]>}.
     *
     * @return the packet result.
     */
    List<byte[]> save();

    /**
     * Puts given data into Cuckoo Filter and traces all modified buckets.
     *
     * @param data data.
     * @return an int set containing all bucket indexes that are modified.
     * @throws IllegalArgumentException if inserting duplicated data into the Cuckoo Filter.
     */
    TIntSet modifyPut(long data);

    default void put(long data) {
        modifyPut(data);
    }

    /**
     * Removes given data from Cuckoo Filter and returns the modified bucket index.
     *
     * @param data data.
     * @return the index of the modified bucket.
     * @throws IllegalArgumentException if removing data that is not contained in the Cuckoo Filter.
     */
    int modifyRemove(long data);

    /**
     * Gets the bucket.
     *
     * @param index index.
     * @return bucket.
     */
    TLongArrayList getBucket(int index);

    /**
     * Gets number of entries per bucket.
     *
     * @return number of entries per bucket.
     */
    int getEntriesPerBucket();

    /**
     * Gets byte length of fingerprint.
     *
     * @return byte length of fingerprint.
     */
    int getFingerprintByteLength();

    /**
     * Gets number of buckets.
     *
     * @return number of buckets.
     */
    int getBucketNum();
}

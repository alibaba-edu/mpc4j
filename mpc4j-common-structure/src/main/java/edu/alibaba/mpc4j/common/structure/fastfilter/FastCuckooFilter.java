package edu.alibaba.mpc4j.common.structure.fastfilter;

import gnu.trove.set.TIntSet;

import java.util.List;

/**
 * fast cuckoo filter.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public interface FastCuckooFilter<T> extends FastCuckooFilterPosition<T> {
    /**
     * Gets number of elements inserted into the random cuckoo filter.
     *
     * @return number of elements inserted into the random cuckoo filter.
     */
    int size();

    /**
     * Tests if the random cuckoo filter might contain data.
     *
     * @param data data.
     * @return true if the random filter might contain data, false otherwise.
     */
    boolean mightContain(T data);

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
    TIntSet modifyPut(T data);

    /**
     * Puts given data into Cuckoo Filter.
     *
     * @param data data.
     */
    void put(T data);

    /**
     * Removes given data from Cuckoo Filter and returns the modified bucket index.
     *
     * @param data data.
     * @return the index of the modified bucket.
     * @throws IllegalArgumentException if removing data that is not contained in the Cuckoo Filter.
     */
    int modifyRemove(T data);

    /**
     * Removes given data from Cuckoo Filter.
     *
     * @param data data.
     * @throws IllegalArgumentException if removing data that is not contained in the Cuckoo Filter.
     */
    default void remove(T data) {
        modifyRemove(data);
    }

    /**
     * Gets table.
     *
     * @return table.
     */
    SingleTable getTable();

    /**
     * Gets the bucket.
     *
     * @param index index.
     * @return bucket.
     */
    default long[] getBucket(int index) {
        return getTable().getBucket(index);
    }

    /**
     * save the part information of a filter.
     *
     * @param fromBucketIndex from which bucket.
     * @param toBucketIndex   to which bucket
     * @return bucket.
     */
    List<byte[]> savePart(int fromBucketIndex, int toBucketIndex);

}

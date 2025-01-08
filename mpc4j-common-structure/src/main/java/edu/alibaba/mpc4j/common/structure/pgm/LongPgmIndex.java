/*
 * HPPC
 *
 * Copyright (C) 2010-2024 Carrot Search s.c. and contributors
 * All rights reserved.
 *
 * Refer to the full license file "LICENSE.txt":
 * https://github.com/carrotsearch/hppc/blob/master/LICENSE.txt
 */
package edu.alibaba.mpc4j.common.structure.pgm;

import com.carrotsearch.hppc.AbstractIterator;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.carrotsearch.hppc.procedures.LongProcedure;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Space-efficient index that enables fast rank/range search operations on a sorted sequence of <code>long</code>.
 * <p>Implementation of the PGM-Index described at
 * <a href="https://pgm.di.unipi.it/">https://pgm.di.unipi.it/</a>, based on the paper
 * <pre>
 *   Paolo Ferragina and Giorgio Vinciguerra.
 *   The PGM-index: a fully-dynamic compressed learned index with provable worst-case bounds.
 *   PVLDB, 13(8): 1162-1175, 2020.
 * </pre>
 * It provides {@code rank} and {@code range} search operations. {@code indexOf()} is faster than B+Tree, and the index
 * is much more compact. {@code contains()} is between 4x to 7x slower than {@code IntHashSet#contains()}, but between
 * 2.5x to 3x faster than {@link Arrays#binarySearch}.
 * <p>
 * Its compactness (40KB for 200MB of keys) makes it efficient for very large collections, the index fitting easily in
 * the L2 cache. The {@code epsilon} parameter should be set according to the desired space-time trade-off. A smaller
 * value makes the estimation more precise and the range smaller but at the cost of increased space usage. In practice,
 * {@code epsilon} 64 is a good sweet spot.
 * <p>
 * Internally the index uses an optimal piecewise linear mapping from keys to their position in the sorted order. This
 * mapping is represented as a sequence of linear models (segments) which are themselves recursively indexed by other
 * piecewise linear mappings.
 *
 * <p> Partly forked from HPPC commit c9497dfabff240787aa0f5ac7a8f4ad70117ea72, and change key type from
 * <code>KType</code> to <code>long</code>.
 *
 * <p> This is only used to construct PGM-index. As shown in
 * <a href="https://github.com/carrotsearch/hppc/pull/39#">#39</a>, bruno-roustant stats that:
 * <pre>
 *     Indeed this PGM-Index is not a collection in itself (though there is a "dynamic" version of it in the paper
 *     that becomes a collection, but it's closer to a Lucene index than a collection for additions and removal). It
 *     clearly benefits from the cool template generation platform, but I agree that it could be moved to a separate
 *     module.
 * </pre>
 * Therefore, the PGM-index (together with IntGrowableArray) is not merged into HPCC itself.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
public class LongPgmIndex {
    /**
     * Empty immutable PGM-Index.
     */
    private static final LongPgmIndex EMPTY = new LongEmptyPgmIndex();

    /**
     * Epsilon approximation range when searching the list of keys.
     * Controls the size of the returned search range, strictly greater than 0.
     * It should be set according to the desired space-time trade-off. A smaller value makes the
     * estimation more precise and the range smaller but at the cost of increased space usage.
     * <p>
     * With EPSILON=64 the benchmark with 200MB of keys shows that this PGM index requires
     * only 2% additional memory on average (40KB). It depends on the distribution of the keys.
     * This epsilon value is good even for 2MB of keys.
     * With EPSILON=32: +5% speed, but 4x space (160KB).
     */
    private static final int EPSILON = 64;
    /**
     * Epsilon approximation range for the segments layers.
     * Controls the size of the search range in the hierarchical segment lists, strictly greater than 0.
     */
    private static final int EPSILON_RECURSIVE = 32;
    /**
     * Size of a key, measured in {@link Integer#BYTES} because the key is stored in an int[].
     */
    private static final int KEY_SIZE = Long.BYTES / Integer.BYTES;
    /**
     * 2x {@link #KEY_SIZE}.
     */
    private static final int DOUBLE_KEY_SIZE = KEY_SIZE * 2;
    /**
     * Data size of a segment, measured in {@link Integer#BYTES}, because segments are stored in an int[].
     */
    private static final int SEGMENT_DATA_SIZE = KEY_SIZE * 3;
    /**
     * Initial value of the exponential jump when scanning out of the epsilon range.
     */
    private static final int BEYOND_EPSILON_JUMP = 16;
    /**
     * The list of keys for which this index is built. It is sorted and may contain duplicate elements.
     */
    private final LongArrayList keys;
    /**
     * The size of the key set. That is, the number of distinct elements in {@link #keys}.
     */
    private final int size;
    /**
     * The lowest key in {@link #keys}.
     */
    private final long firstKey;
    /**
     * The highest key in {@link #keys}.
     */
    private final long lastKey;
    /**
     * The epsilon range used to build this index.
     */
    private final int epsilon;
    /**
     * The recursive epsilon range used to build this index.
     */
    private final int epsilonRecursive;
    /**
     * The offsets in {@link #segmentData} of the first segment of each segment level.
     */
    private final int[] levelOffsets;
    /**
     * The index data. It contains all the segments for all the levels.
     */
    private final int[] segmentData;

    /**
     * Creates <code>LongPgmIndex</code> from <code>byte[] data</code>.
     *
     * @param data data.
     * @return an instance.
     */
    public static LongPgmIndex fromByteArray(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        // read keys
        int keySize = byteBuffer.getInt();
        if (keySize == 0) {
            // keys.isEmpty()
            return EMPTY;
        }
        LongArrayList keys = new LongArrayList(keySize);
        for (int i = 0; i < keySize; i++) {
            keys.add(byteBuffer.getLong());
        }
        // read size
        int size = byteBuffer.getInt();
        // read epsilon range
        int epsilon = byteBuffer.getInt();
        // read recursive epsilon range
        int epsilonRecursive = byteBuffer.getInt();
        // read level offsets
        int levelOffsetSize = byteBuffer.getInt();
        int[] levelOffsets = new int[levelOffsetSize];
        for (int i = 0; i < levelOffsetSize; i++) {
            levelOffsets[i] = byteBuffer.getInt();
        }
        // read segment data
        int segmentDataSize = byteBuffer.getInt();
        int[] segmentData = new int[segmentDataSize];
        for (int i = 0; i < segmentDataSize; i++) {
            segmentData[i] = byteBuffer.getInt();
        }
        return new LongPgmIndex(keys, size, epsilon, epsilonRecursive, levelOffsets, segmentData);
    }

    private LongPgmIndex(LongArrayList keys, int size, int epsilon, int epsilonRecursive,
                         int[] levelOffsets, int[] segmentData) {
        assert !keys.isEmpty();
        assert size > 0 && size <= keys.size();
        assert epsilon > 0;
        assert epsilonRecursive > 0;
        this.keys = keys;
        this.size = size;
        firstKey = keys.get(0);
        lastKey = keys.get(keys.size() - 1);
        this.epsilon = epsilon;
        this.epsilonRecursive = epsilonRecursive;
        this.levelOffsets = levelOffsets;
        this.segmentData = segmentData;
    }

    /**
     * Serializes the PGM-index.
     *
     * @return serialized result.
     */
    public byte[] toByteArray() {
        // handle empty PGM-index
        if (keys.isEmpty()) {
            return ByteBuffer.allocate(Integer.BYTES).putInt(keys.size()).array();
        }
        // handle non-empty PGM-index
        ByteBuffer byteBuffer = ByteBuffer.allocate(
            // LongArrayList keys
            Integer.BYTES + keys.size() * Long.BYTES
                // int size, int epsilon, int epsilonRecursive
                + Integer.BYTES + Integer.BYTES + Integer.BYTES
                // int[] levelOffsets
                + Integer.BYTES + levelOffsets.length * Integer.BYTES
                // int[] segmentData
                + Integer.BYTES + segmentData.length * Integer.BYTES
        );
        // LongArrayList keys
        byteBuffer.putInt(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            byteBuffer.putLong(keys.get(i));
        }
        // int size, int epsilon, int epsilonRecursive
        byteBuffer.putInt(size);
        byteBuffer.putInt(epsilon);
        byteBuffer.putInt(epsilonRecursive);
        // int[] levelOffsets
        byteBuffer.putInt(levelOffsets.length);
        for (int levelOffset : levelOffsets) {
            byteBuffer.putInt(levelOffset);
        }
        // int[] segmentData
        byteBuffer.putInt(segmentData.length);
        for (int segmentDatum : segmentData) {
            byteBuffer.putInt(segmentDatum);
        }
        return byteBuffer.array();
    }

    /**
     * Empty set constructor.
     */
    private LongPgmIndex() {
        keys = new LongArrayList(0);
        size = 0;
        firstKey = LongIntrinsics.empty();
        lastKey = LongIntrinsics.empty();
        epsilon = 0;
        epsilonRecursive = 0;
        levelOffsets = new int[0];
        segmentData = levelOffsets;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(keys.stream().toArray())
            .append(size)
            .append(epsilon)
            .append(epsilonRecursive)
            .append(levelOffsets)
            .append(segmentData)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LongPgmIndex that) {
            return new EqualsBuilder()
                .append(this.keys.stream().toArray(), that.keys.stream().toArray())
                .append(this.size, that.size)
                .append(this.epsilon, that.epsilon)
                .append(this.epsilonRecursive, that.epsilonRecursive)
                .append(this.levelOffsets, that.levelOffsets)
                .append(this.segmentData, that.segmentData)
                .isEquals();
        }
        return false;
    }

    /**
     * Returns the size of the key set. That is, the number of distinct elements in {@link #keys}.
     */
    public int size() {
        return size;
    }

    /**
     * Returns whether this key set is empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns whether this key set contains the given key.
     */
    public boolean contains(long key) {
        return indexOf(key) >= 0;
    }

    /**
     * Searches the specified key, and returns its index in the element list.
     * If multiple elements are equal to the specified key, there is no
     * guarantee which one will be found.
     *
     * @return The index of the searched key if it is present;
     * otherwise, {@code (-(<i>insertion point</i>) - 1)}. The
     * <i>insertion point</i> is defined as the point at which the
     * key would be inserted into the list: the index of the first
     * element greater than the key, or {@link #keys}#{@code size()}
     * if all the elements are less than the specified key. Note that
     * this guarantees that the return value will be &gt;= 0 if and
     * only if the key is found.
     */
    public int indexOf(long key) {
        if (LongIntrinsics.compare(key, firstKey) < 0) {
            return -1;
        }
        if (LongIntrinsics.compare(key, lastKey) > 0) {
            return -keys.size() - 1;
        }
        final int[] segmentData = this.segmentData;
        int segmentDataIndex = findSegment(key);
        int nextIntercept = (int) getIntercept(segmentDataIndex + SEGMENT_DATA_SIZE, segmentData);
        int index = Math.min(approximateIndex(key, segmentDataIndex, segmentData), Math.min(nextIntercept, keys.size() - 1));
        assert index >= 0 && index < keys.size();
        long k = keys.get(index);
        if (LongIntrinsics.compare(key, k) < 0) {
            // Scan sequentially before the approximated index, within epsilon range.
            final int fromIndex = Math.max(index - epsilon - 1, 0);
            while (--index >= fromIndex) {
                k = keys.get(index);
                if (LongIntrinsics.compare(key, k) > 0) {
                    return -index - 2;
                }
                if (LongIntrinsics.compare(key, k) == 0) {
                    return index;
                }
            }
            // Continue scanning out of the epsilon range.
            // This might happen in rare cases of precision error during the approximation
            // computation for longs (we don't have long double 128 bits in Java).
            // This might also happen in rare corner cases of large duplicate elements
            // sequence at the epsilon range boundary.
            index++;
            int jump = BEYOND_EPSILON_JUMP;
            do {
                int loIndex = Math.max(index - jump, 0);
                if (LongIntrinsics.compare(key, keys.get(loIndex)) >= 0) {
                    return Arrays.binarySearch(keys.buffer, loIndex, index, key);
                }
                index = loIndex;
                jump <<= 1;
            } while (index > 0);
            return -1;
        } else if (LongIntrinsics.compare(key, k) == 0) {
            return index;
        } else {
            // Scan sequentially after the approximated index, within epsilon range.
            final int toIndex = Math.min(index + epsilon + 3, keys.size());
            while (++index < toIndex) {
                k = keys.get(index);
                if (LongIntrinsics.compare(key, k) < 0) {
                    return -index - 1;
                }
                if (LongIntrinsics.compare(key, k) == 0) {
                    return index;
                }
            }
            // Continue scanning out of the epsilon range.
            int jump = BEYOND_EPSILON_JUMP;
            do {
                int hiIndex = Math.min(index + jump, keys.size());
                if (LongIntrinsics.compare(key, keys.get(hiIndex)) <= 0) {
                    return Arrays.binarySearch(keys.buffer, index, hiIndex, key);
                }
                index = hiIndex;
                jump <<= 1;
            } while (index < keys.size());
            return -keys.size() - 1;
        }
    }

    /**
     * Returns, for any value {@code x}, the number of keys in the sorted list
     * which are smaller than {@code x}.
     * It is equal to {@link #indexOf} if {@code x} belongs to the list,
     * or -{@link #indexOf}-1 otherwise.
     *
     * <p>If multiple elements are equal to the specified key, there is no
     * guarantee which one will be found.
     *
     * @return The index of the searched key if it is present;
     * otherwise, the {@code insertion point}. The
     * <i>insertion point</i> is defined as the point at which the
     * key would be inserted into the list: the index of the first
     * element greater than the key, or {@link #keys}#{@code size()}
     * if all the elements are less than the specified key. Note that
     * this method always returns a value &gt;= 0.
     */
    public int rank(long x) {
        int index = indexOf(x);
        return index >= 0 ? index : -index - 1;
    }

    /**
     * Returns the number of keys in the list that are greater than or equal to
     * {@code minKey} (inclusive), and less than or equal to {@code maxKey} (inclusive).
     */
    public int rangeCardinality(long minKey, long maxKey) {
        int fromIndex = rank(minKey);
        int maxIndex = indexOf(maxKey);
        int toIndex = maxIndex >= 0 ? maxIndex + 1 : -maxIndex - 1;
        return Math.max(toIndex - fromIndex, 0);
    }

    /**
     * Returns an iterator over the keys in the list that are greater than or equal to
     * {@code minKey} (inclusive), and less than or equal to {@code maxKey} (inclusive).
     */
    public Iterator<LongCursor> rangeIterator(long minKey, long maxKey) {
        int fromIndex = rank(minKey);
        return new RangeIterator(keys, fromIndex, maxKey);
    }

    /**
     * Applies {@code procedure} to the keys in the list that are greater than or equal
     * to {@code minKey} (inclusive), and less than or equal to {@code maxKey} (inclusive).
     */
    public void forEachInRange(LongProcedure procedure, long minKey, long maxKey) {
        final long[] buffer = keys.buffer;
        long k;
        for (int i = rank(minKey), size = keys.size(); i < size && LongIntrinsics.compare((k = buffer[i]), maxKey) <= 0; i++) {
            procedure.apply(k);
        }
    }

    /**
     * Finds the segment responsible for a given key, that is,
     * the rightmost segment having its first key <= the searched key.
     *
     * @return the segment data index; or -1 if none.
     */
    private int findSegment(long key) {
        assert LongIntrinsics.compare(key, firstKey) >= 0 && LongIntrinsics.compare(key, lastKey) <= 0;
        final int epsilonRecursive = this.epsilonRecursive;
        final int[] levelOffsets = this.levelOffsets;
        final int[] segmentData = this.segmentData;
        int level = levelOffsets.length - 1;
        int segmentDataIndex = levelOffsets[level] * SEGMENT_DATA_SIZE;
        while (--level >= 0) {
            int nextIntercept = (int) getIntercept(segmentDataIndex + SEGMENT_DATA_SIZE, segmentData);
            int index = Math.min(approximateIndex(key, segmentDataIndex, segmentData), nextIntercept);
            assert index >= 0 && index <= levelOffsets[level + 1] - levelOffsets[level] - 1;
            int sdIndex = (levelOffsets[level] + index) * SEGMENT_DATA_SIZE;
            if (LongIntrinsics.compare(getKey(sdIndex, segmentData), key) <= 0) {
                // Scan sequentially segments after the approximated index, within the epsilon range.
                final int levelNumSegments = levelOffsets[level + 1] - levelOffsets[level] - 1;
                final int toIndex = Math.min(index + epsilonRecursive + 3, levelNumSegments);
                while (index++ < toIndex
                    && LongIntrinsics.compare(getKey(sdIndex + SEGMENT_DATA_SIZE, segmentData), key) <= 0) {
                    sdIndex += SEGMENT_DATA_SIZE;
                }
            } else {
                // Scan sequentially segments before the approximated index, within the epsilon range.
                final int fromIndex = Math.max(index - epsilonRecursive - 1, 0);
                while (index-- > fromIndex) {
                    sdIndex -= SEGMENT_DATA_SIZE;
                    if (LongIntrinsics.compare(getKey(sdIndex, segmentData), key) <= 0) {
                        break;
                    }
                }
            }
            segmentDataIndex = sdIndex;
        }
        assert segmentDataIndex >= 0;
        return segmentDataIndex;
    }

    private int approximateIndex(long key, int segmentDataIndex, int[] segmentData) {
        long intercept = getIntercept(segmentDataIndex, segmentData);
        long sKey = getKey(segmentDataIndex, segmentData);
        double slope = getSlope(segmentDataIndex, segmentData);
        int index = (int) (slope * (LongIntrinsics.numeric(key) - LongIntrinsics.numeric(sKey)) + intercept);
        return Math.max(index, 0);
    }

    private static long getIntercept(int segmentDataIndex, int[] segmentData) {
        return PgmIndexUtil.getIntercept(segmentDataIndex, segmentData);
    }

    private long getKey(int segmentDataIndex, int[] segmentData) {
        return PgmIndexUtil.getLongKey(segmentDataIndex + KEY_SIZE, segmentData);
    }

    private static double getSlope(int segmentDataIndex, int[] segmentData) {
        return PgmIndexUtil.getSlope(segmentDataIndex + DOUBLE_KEY_SIZE, segmentData);
    }

    /**
     * Empty immutable PGM Index.
     */
    private static class LongEmptyPgmIndex extends LongPgmIndex {

        private final Iterator<LongCursor> emptyIterator = new LongEmptyIterator();

        @Override
        public int indexOf(long key) {
            return -1;
        }

        @Override
        public Iterator<LongCursor> rangeIterator(long minKey, long maxKey) {
            return emptyIterator;
        }

        @Override
        public void forEachInRange(LongProcedure procedure, long minKey, long maxKey) {
            // empty
        }

        private static class LongEmptyIterator extends AbstractIterator<LongCursor> {
            @Override
            protected LongCursor fetch() {
                return done();
            }
        }
    }

    /**
     * Iterator over a range of elements in a sorted array.
     */
    protected static class RangeIterator extends AbstractIterator<LongCursor> {
        private final long[] buffer;
        private final int size;
        private final LongCursor cursor;
        private final long maxKey;

        /**
         * Range iterator from {@code fromIndex} (inclusive) to {@code maxKey} (inclusive).
         */
        protected RangeIterator(LongArrayList keys, int fromIndex, long maxKey) {
            this.buffer = keys.buffer;
            this.size = keys.size();
            this.cursor = new LongCursor();
            this.cursor.index = fromIndex;
            this.maxKey = maxKey;
        }

        @Override
        protected LongCursor fetch() {
            if (cursor.index >= size) {
                return done();
            }
            cursor.value = buffer[cursor.index++];
            if (LongIntrinsics.compare(cursor.value, maxKey) > 0) {
                cursor.index = size;
                return done();
            }
            return cursor;
        }
    }

    /**
     * Builds a {@link LongPgmIndex} on a provided sorted list of keys.
     */
    public static class LongPgmIndexBuilder implements PlaModel.SegmentConsumer {
        protected LongArrayList keys;
        protected int epsilon = EPSILON;
        protected int epsilonRecursive = EPSILON_RECURSIVE;
        protected PlaModel plam;
        protected int size;
        protected IntGrowableArray segmentData;
        protected int numSegments;

        /**
         * Sets the sorted list of keys to build the index for; duplicate elements are allowed.
         */
        public LongPgmIndexBuilder setSortedKeys(LongArrayList keys) {
            this.keys = keys;
            return this;
        }

        /**
         * Sets the sorted array of keys to build the index for; duplicate elements are allowed.
         */
        public LongPgmIndexBuilder setSortedKeys(long[] keys, int length) {
            LongArrayList keyList = new LongArrayList(0);
            keyList.buffer = keys;
            keyList.elementsCount = length;
            return setSortedKeys(keyList);
        }

        /**
         * Sets the epsilon range to use when learning the segments for the list of keys.
         */
        public LongPgmIndexBuilder setEpsilon(int epsilon) {
            if (epsilon <= 0) {
                throw new IllegalArgumentException("epsilon must be > 0");
            }
            this.epsilon = epsilon;
            return this;
        }

        /**
         * Sets the recursive epsilon range to use when learning the segments for the segment levels.
         */
        public LongPgmIndexBuilder setEpsilonRecursive(int epsilonRecursive) {
            if (epsilonRecursive <= 0) {
                throw new IllegalArgumentException("epsilonRecursive must be > 0");
            }
            this.epsilonRecursive = epsilonRecursive;
            return this;
        }

        /**
         * Builds the {@link LongPgmIndex}; or {@link #EMPTY} if there are no keys in the list.
         */
        public LongPgmIndex build() {
            if (keys == null || keys.isEmpty()) {
                return EMPTY;
            }
            plam = new PlaModel(epsilon);

            int segmentsInitialCapacity = Math.min(Math.max(keys.size() / (2 * epsilon * epsilon) * SEGMENT_DATA_SIZE, 16), 1 << 19);
            segmentData = new IntGrowableArray(segmentsInitialCapacity);
            IntGrowableArray levelOffsets = new IntGrowableArray(16);

            int levelOffset = 0;
            levelOffsets.add(levelOffset);
            int levelNumSegments = buildFirstLevel();
            while (levelNumSegments > 1) {
                int nextLevelOffset = numSegments;
                levelOffsets.add(nextLevelOffset);
                levelNumSegments = buildUpperLevel(levelOffset, levelNumSegments);
                levelOffset = nextLevelOffset;
            }

            int[] segmentDataFinal = segmentData.toArray();
            int[] levelOffsetsFinal = levelOffsets.toArray();
            return new LongPgmIndex(keys, size, epsilon, epsilonRecursive, levelOffsetsFinal, segmentDataFinal);
        }

        private int buildFirstLevel() {
            assert numSegments == 0;
            int numKeys = keys.size();
            int size = 0;
            long key = keys.get(0);
            size++;
            plam.addKey(key, 0, this);
            for (int i = 1; i < numKeys; i++) {
                long nextKey = keys.get(i);
                if (LongIntrinsics.compare(nextKey, key) != 0) {
                    key = nextKey;
                    plam.addKey(key, i, this);
                    size++;
                }
            }
            plam.finish(this);
            addSentinelSegment(numKeys);
            this.size = size;
            return numSegments - 1;
        }

        private int buildUpperLevel(int levelOffset, int levelNumSegments) {
            plam.setEpsilon(epsilonRecursive);
            assert numSegments > 0;
            int initialNumSegments = numSegments;
            int segmentDataIndex = levelOffset * SEGMENT_DATA_SIZE;
            long key = getKey(segmentDataIndex, segmentData.buffer);
            plam.addKey(key, 0, this);
            for (int i = 1; i < levelNumSegments; i++) {
                segmentDataIndex += SEGMENT_DATA_SIZE;
                long nextKey = getKey(segmentDataIndex, segmentData.buffer);
                if (LongIntrinsics.compare(nextKey, key) != 0) {
                    key = nextKey;
                    plam.addKey(key, i, this);
                }
            }
            plam.finish(this);
            addSentinelSegment(levelNumSegments);
            return numSegments - initialNumSegments - 1;
        }

        private long getKey(int segmentDataIndex, int[] segmentData) {
            return PgmIndexUtil.getLongKey(segmentDataIndex + KEY_SIZE, segmentData);
        }

        /**
         * Adds a sentinel segment that is used to give a limit for the position approximation,
         * but does not count in the number of segments per level.
         */
        private void addSentinelSegment(int endIndex) {
            // This sentinel segment is used in findSegment().
            accept(Long.MAX_VALUE, 0d, endIndex);
        }

        @Override
        public void accept(long firstKey, double slope, long intercept) {
            PgmIndexUtil.addIntercept(intercept, segmentData);
            PgmIndexUtil.addLongKey(firstKey, segmentData);
            PgmIndexUtil.addSlope(slope, segmentData);
            numSegments++;
            assert segmentData.size == numSegments * SEGMENT_DATA_SIZE;
        }
    }
}

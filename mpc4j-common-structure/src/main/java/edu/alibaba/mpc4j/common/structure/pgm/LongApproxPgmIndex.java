package edu.alibaba.mpc4j.common.structure.pgm;

import com.carrotsearch.hppc.LongArrayList;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;

/**
 * Space-efficient index that enables fast rank/range search operations on a sorted sequence of <code>long</code>.
 *
 * <p> This is done based on <code>LongPgmIndex</code>, except that this does not store the keys to generate the index.
 *
 * @author Weiran Liu
 * @date 2024/8/1
 */
public class LongApproxPgmIndex {
    /**
     * Empty immutable PGM-Index.
     */
    private static final LongApproxPgmIndex EMPTY = new EmptyLongApproxPgmIndex();

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
     * the size of keys used to build this index.
     */
    private final int keySize;
    /**
     * The size of the key set. That is, the number of distinct elements used to build this index.
     */
    private final int size;
    /**
     * The lowest key used to build this index.
     */
    private final long firstKey;
    /**
     * The highest key used to build this index.
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
    public static LongApproxPgmIndex fromByteArray(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        // read keySize
        int keySize = byteBuffer.getInt();
        if (keySize == 0) {
            // handle empty PGM-index
            return EMPTY;
        }
        // read size
        int size = byteBuffer.getInt();
        // read epsilon range
        int epsilon = byteBuffer.getInt();
        // read recursive epsilon range
        int epsilonRecursive = byteBuffer.getInt();
        // read first and last key
        long firstKey = byteBuffer.getLong();
        long lastKey = byteBuffer.getLong();
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
        return new LongApproxPgmIndex(keySize, size, epsilon, epsilonRecursive, firstKey, lastKey, levelOffsets, segmentData);
    }

    private LongApproxPgmIndex(int keySize, int size, int epsilon, int epsilonRecursive, long firstKey, long lastKey,
                               int[] levelOffsets, int[] segmentData) {
        assert keySize > 0;
        assert size > 0 && size <= keySize;
        assert epsilon > 0;
        assert epsilonRecursive > 0;
        this.keySize = keySize;
        this.size = size;
        this.epsilon = epsilon;
        this.epsilonRecursive = epsilonRecursive;
        this.firstKey = firstKey;
        this.lastKey = lastKey;
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
        if (keySize == 0) {
            return ByteBuffer.allocate(Integer.BYTES).putInt(keySize).array();
        }
        // handle non-empty PGM-index
        ByteBuffer byteBuffer = ByteBuffer.allocate(
            // int keySize, int size, int epsilon, int epsilonRecursive
            Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES
                // first key, last key
                + Long.BYTES + Long.BYTES
                // int[] levelOffsets
                + Integer.BYTES + levelOffsets.length * Integer.BYTES
                // int[] segmentData
                + Integer.BYTES + segmentData.length * Integer.BYTES
        );
        // int keySize, int size, int epsilon, int epsilonRecursive
        byteBuffer.putInt(keySize);
        byteBuffer.putInt(size);
        byteBuffer.putInt(epsilon);
        byteBuffer.putInt(epsilonRecursive);
        // int firstKey, int lastKey
        byteBuffer.putLong(firstKey);
        byteBuffer.putLong(lastKey);
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
    private LongApproxPgmIndex() {
        keySize = 0;
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
            .append(keySize)
            .append(size)
            .append(epsilon)
            .append(epsilonRecursive)
            .append(firstKey)
            .append(lastKey)
            .append(levelOffsets)
            .append(segmentData)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LongApproxPgmIndex that) {
            return new EqualsBuilder()
                .append(this.keySize, that.keySize)
                .append(this.size, that.size)
                .append(this.epsilon, that.epsilon)
                .append(this.epsilonRecursive, that.epsilonRecursive)
                .append(this.firstKey, that.firstKey)
                .append(this.lastKey, that.lastKey)
                .append(this.levelOffsets, that.levelOffsets)
                .append(this.segmentData, that.segmentData)
                .isEquals();
        }
        return false;
    }

    /**
     * Returns the size of the key set. That is, the number of distinct elements used to build the index.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the number of segments in the first level.
     *
     * @return the number of segments in the first level.
     */
    public int firstLevelSegmentNum() {
        // each segment data is [intercept, key, slope], totally 6 integers.
        return segmentData.length / 6;
    }

    /**
     * Returns whether this key set is empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Searches the specified key, and returns its approximate index range in the element list.
     *
     * @return The approximate index range of the searched key. If the specified key is less than the first key,
     * return [-1, -1]; If the specified key is greater than the last key, return [-size - 1, -size - 1].
     */
    public int[] approximateIndexRangeOf(long key) {
        if (LongIntrinsics.compare(key, firstKey) < 0) {
            return new int[] {-1, -1, -1};
        }
        if (LongIntrinsics.compare(key, lastKey) > 0) {
            return new int[] {-keySize - 1, -keySize - 1, -keySize - 1};
        }
        final int[] segmentData = this.segmentData;
        int segmentDataIndex = findSegment(key);
        int nextIntercept = (int) getIntercept(segmentDataIndex + SEGMENT_DATA_SIZE, segmentData);
        int index = Math.min(approximateIndex(key, segmentDataIndex, segmentData), Math.min(nextIntercept, keySize - 1));
        assert index >= 0 && index < keySize;
        // with very high probability, the actual index is in range
        // Math.max(index - epsilon - 1, 0), Math.min(index + epsilon + 3, keySize);
        // With rare cases, index is out of that range due to precision error during the approximation for longs
        // we don't have long double 128 bits in Java).
        return new int[] {index, Math.max(index - epsilon - 1, 0), Math.min(index + epsilon + 3, keySize)};
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
    private static class EmptyLongApproxPgmIndex extends LongApproxPgmIndex {

        @Override
        public int[] approximateIndexRangeOf(long key) {
            return new int[] {-1, -1};
        }
    }

    /**
     * Builds a {@link LongApproxPgmIndex} on a provided sorted list of keys.
     */
    public static class LongApproxPgmIndexBuilder implements PlaModel.SegmentConsumer {
        protected LongArrayList keys;
        protected int epsilon = EPSILON;
        protected int epsilonRecursive = EPSILON_RECURSIVE;
        protected PlaModel plam;
        protected int size;
        protected IntGrowableArray segmentData;
        protected int numSegments;

        /**
         * Sets the sorted list of keys to build the index for; duplicate elements are not allowed.
         */
        public LongApproxPgmIndexBuilder setSortedKeys(LongArrayList keys) {
            long distinctCount = keys.stream().distinct().count();
            MathPreconditions.checkEqual("distinct_count", "keys.size()", distinctCount, keys.size());
            this.keys = keys;
            return this;
        }

        /**
         * Sets the sorted array of keys to build the index for; duplicate elements are not allowed.
         */
        public LongApproxPgmIndexBuilder setSortedKeys(long[] keys) {
            LongArrayList keyList = new LongArrayList(0);
            keyList.buffer = keys;
            keyList.elementsCount = keys.length;
            return setSortedKeys(keyList);
        }

        /**
         * Sets the epsilon range to use when learning the segments for the list of keys.
         */
        public LongApproxPgmIndexBuilder setEpsilon(int epsilon) {
            if (epsilon <= 0) {
                throw new IllegalArgumentException("epsilon must be > 0");
            }
            this.epsilon = epsilon;
            return this;
        }

        /**
         * Sets the recursive epsilon range to use when learning the segments for the segment levels.
         */
        public LongApproxPgmIndexBuilder setEpsilonRecursive(int epsilonRecursive) {
            if (epsilonRecursive <= 0) {
                throw new IllegalArgumentException("epsilonRecursive must be > 0");
            }
            this.epsilonRecursive = epsilonRecursive;
            return this;
        }

        /**
         * Builds the {@link LongApproxPgmIndex}; or {@link #EMPTY} if there are no keys in the list.
         */
        public LongApproxPgmIndex build() {
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
            return new LongApproxPgmIndex(
                keys.size(), size, epsilon, epsilonRecursive, keys.get(0), keys.get(keys.size() - 1),
                levelOffsetsFinal, segmentDataFinal
            );
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

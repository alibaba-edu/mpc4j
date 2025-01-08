package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * mobile fast vacuum filter.
 *
 * @author Weiran Liu
 * @date 2024/11/8
 */
public class MobileFastVacuumFilter<T> extends AbstractFastVacuumFilter<T> {
    /**
     * fast cuckoo filter type
     */
    private static final FastCuckooFilterType TYPE = MobileFastVacuumFilterPosition.TYPE;
    /**
     * α, number of elements in buckets / total number of elements. Experiments show that we cannot set α = 0.66 since
     * there will be non-negligible failure probabilities. We have to set α = 0.955.
     */
    private static final double LOAD_FACTOR = MobileFastVacuumFilterPosition.LOAD_FACTOR;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BIT_LENGTH = MobileFastVacuumFilterPosition.FINGERPRINT_BIT_LENGTH;

    static <X> MobileFastVacuumFilter<X> create(int maxSize, long hashSeed) {
        return new MobileFastVacuumFilter<>(maxSize, hashSeed);
    }

    static <X> MobileFastVacuumFilter<X> load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 4);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read hash seed
        long hashSeed = headerByteBuffer.getLong();
        // read max size
        int maxSize = headerByteBuffer.getInt();
        MobileFastVacuumFilter<X> filter = new MobileFastVacuumFilter<>(maxSize, hashSeed);
        // read size
        filter.size = headerByteBuffer.getInt();
        // read elements
        BitVector bitmap = BitVectorFactory.create(3 * filter.bucketNum, byteArrayList.remove(0));
        byte[] compressed = byteArrayList.remove(0);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, filter.fingerprintByteLength).toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int bucketIndex = 0, offset = 0; bucketIndex < filter.bucketNum; bucketIndex++) {
            int count = (bitmap.get(offset) ? 4 : 0) + (bitmap.get(offset + 1) ? 2 : 0) + (bitmap.get(offset + 2) ? 1 : 0);
            for (int entryIndex = 0; entryIndex < count; entryIndex++) {
                filter.table.insertIndexToBucket(bucketIndex, entryIndex, LongUtils.fixedByteArrayToLong(bucketFlattenedElements[flattenedIndex]));
                flattenedIndex++;
            }
            offset += 3;
        }

        return filter;
    }

    private MobileFastVacuumFilter(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, FINGERPRINT_BIT_LENGTH, LOAD_FACTOR);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 2 + Long.BYTES);
        // hash seed
        headerByteBuffer.putLong(hashSeed);
        // max size
        headerByteBuffer.putInt(maxSize);
        // size
        headerByteBuffer.putInt(size);
        byteArrayList.add(headerByteBuffer.array());

        List<byte[]> cuckooFilterList = new LinkedList<>();
        // we concurrently encode bitmap and entries.
        int bitmapSize = 3 * bucketNum;
        // 2 bit store (the number of fingerprints in bucket) - 1
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        int offset = 0;
        for (int i = 0; i < bucketNum; i++) {
            int count = 0;
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                if (fingerprint != 0L) {
                    cuckooFilterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
                    count++;
                } else {
                    break;
                }
            }
            bitmap.set(offset, count >= 4);
            bitmap.set(offset + 1, count >= 2 && count < 4);
            bitmap.set(offset + 2, (count & 1) == 1);
            offset += 3;
        }
        byteArrayList.add(bitmap.getBytes());
        byteArrayList.add(SerializeUtils.compressEqual(cuckooFilterList, fingerprintByteLength));

        return byteArrayList;
    }

    /**
     * Load part data of fast cuckoo filter based on {@code List<byte[]>}.
     *
     * @param partMsg the filter represented by {@code List<byte[]>}.
     * @return the fingerprints
     */
    static long[][] loadPart(List<byte[]> partMsg){
        MathPreconditions.checkEqual("actual list size", "expect list size", partMsg.size(), 3);
        int bucketNum = IntUtils.byteArrayToInt(partMsg.remove(0));
        long[][] fingerprints = new long[bucketNum][ENTRIES_PER_BUCKET];
        // read elements
        BitVector bitmap = BitVectorFactory.create(3 * bucketNum, partMsg.remove(0));
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int i = 0; i < bucketNum; i++) {
            int offset = i * 3;
            int count = (bitmap.get(offset) ? 4 : 0) + (bitmap.get(offset + 1) ? 2 : 0) + (bitmap.get(offset + 2) ? 1 : 0);
            for (int j = 0; j < count; j++) {
                fingerprints[i][j] = LongUtils.fixedByteArrayToLong(bucketFlattenedElements[flattenedIndex]);
                flattenedIndex++;
            }
        }
        return fingerprints;
    }

    /**
     * Load part data of fast cuckoo filter in byte array form based on {@code List<byte[]>}.
     *
     * @param partMsg the filter represented by {@code List<byte[]>}.
     * @return the fingerprints
     */
    static byte[][] loadPartByte(List<byte[]> partMsg){
        MathPreconditions.checkEqual("actual list size", "expect list size", partMsg.size(), 3);
        int bucketNum = IntUtils.byteArrayToInt(partMsg.remove(0));
        byte[][] fingerprints = new byte[bucketNum][];
        // read elements
        BitVector bitmap = BitVectorFactory.create(3 * bucketNum, partMsg.remove(0));
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int i = 0; i < bucketNum; i++) {
            int offset = i * 3;
            int count = (bitmap.get(offset) ? 4 : 0) + (bitmap.get(offset + 1) ? 2 : 0) + (bitmap.get(offset + 2) ? 1 : 0);
            fingerprints[i] = new byte[count * fingerprintByteLength];
            for (int j = 0; j < count; j++) {
                System.arraycopy(bucketFlattenedElements[flattenedIndex], 0, fingerprints[i], j * fingerprintByteLength, fingerprintByteLength);
                flattenedIndex++;
            }
        }
        return fingerprints;
    }

    @Override
    public List<byte[]> savePart(int fromBucketIndex, int toBucketIndex) {
        assert fromBucketIndex >= 0 && toBucketIndex <= bucketNum && fromBucketIndex < toBucketIndex;
        int saveBucketNum = toBucketIndex - fromBucketIndex;
        List<byte[]> byteArrayList = new LinkedList<>();
        int bitmapSize = 3 * saveBucketNum;
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        List<byte[]> filterList = new LinkedList<>();
        for (int i = fromBucketIndex, indicatorIndex = 0; i < toBucketIndex; i++) {
            int count = 0;
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                if (fingerprint != 0L) {
                    count++;
                    filterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
                } else {
                    break;
                }
            }
            bitmap.set(indicatorIndex, count >= 4);
            bitmap.set(indicatorIndex + 1, count >= 2 && count < 4);
            bitmap.set(indicatorIndex + 2, (count & 1) == 1);
            indicatorIndex += 3;
        }
        byteArrayList.add(IntUtils.intToByteArray(saveBucketNum));
        byteArrayList.add(bitmap.getBytes());
        byteArrayList.add(SerializeUtils.compressEqual(filterList, fingerprintByteLength));
        return byteArrayList;
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}

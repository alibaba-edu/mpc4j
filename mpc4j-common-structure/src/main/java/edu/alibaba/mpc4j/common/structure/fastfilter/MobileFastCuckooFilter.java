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
 * mobile fast cuckoo filter.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public class MobileFastCuckooFilter<T> extends AbstractFastCuckooFilter<T> {
    /**
     * fast cuckoo filter type
     */
    private static final FastCuckooFilterType TYPE = MobileFastCuckooFilterPosition.TYPE;
    /**
     * b = 3.
     */
    private static final int ENTRIES_PER_BUCKET = MobileFastCuckooFilterPosition.ENTRIES_PER_BUCKET;
    /**
     * v = 32.
     */
    private static final int FINGERPRINT_BIT_LENGTH = MobileFastCuckooFilterPosition.FINGERPRINT_BIT_LENGTH;


    /**
     * Creates an empty fast cuckoo filter.
     *
     * @param maxSize  max number of inserted elements.
     * @param hashSeed hash seed.
     * @return an empty fast cuckoo filter.
     */
    static <X> MobileFastCuckooFilter<X> create(int maxSize, long hashSeed) {
        return new MobileFastCuckooFilter<>(maxSize, hashSeed);
    }

    /**
     * Creates the fast cuckoo filter based on {@code List<byte[]>}.
     *
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the fast cuckoo filter.
     */
    static <X> MobileFastCuckooFilter<X> load(List<byte[]> byteArrayList) {
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
        MobileFastCuckooFilter<X> filter = new MobileFastCuckooFilter<>(maxSize, hashSeed);
        // read size
        filter.size = headerByteBuffer.getInt();
        // read elements
        BitVector bitmap = BitVectorFactory.create(2 * filter.bucketNum, byteArrayList.remove(0));
        byte[] compressed = byteArrayList.remove(0);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, filter.fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int i = 0; i < filter.bucketNum; i++) {
            int offset = i * 2;
            int count = (bitmap.get(offset) ? 2 : 0) + (bitmap.get(offset + 1) ? 1 : 0);
            for (int j = 0; j < count; j++) {
                filter.table.insertIndexToBucket(i, j, LongUtils.fixedByteArrayToLong(bucketFlattenedElements[flattenedIndex]));
                flattenedIndex++;
            }
        }
        return filter;
    }

    private MobileFastCuckooFilter(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, ENTRIES_PER_BUCKET, FINGERPRINT_BIT_LENGTH);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES * 2);
        // write hash seed
        headerByteBuffer.putLong(hashSeed);
        // write max size
        headerByteBuffer.putInt(maxSize);
        // write size
        headerByteBuffer.putInt(size);
        byteArrayList.add(headerByteBuffer.array());
        // Optimizations for mobile filter. See USENIX 2019 paper, Section 4.1. For each entry of a Cuckoo filter, an
        // additional bit is transmitted that indicates whether this entry is empty or holds a tag. The entry itself is
        // only transmitted if it is not empty.
        List<byte[]> cuckooFilterList = new LinkedList<>();
        // we concurrently encode bitmap and entries.
        int bitmapSize = 2 * bucketNum;
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        for (int i = 0; i < bucketNum; i++) {
            int count = 0;
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                if (fingerprint != 0L) {
                    count++;
                    cuckooFilterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
                } else {
                    break;
                }
            }
            int offset = i * 2;
            if (count >= 2) {
                bitmap.set(offset, true);
            }
            if ((count & 1) == 1) {
                bitmap.set(offset + 1, true);
            }
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
        BitVector bitmap = BitVectorFactory.create(2 * bucketNum, partMsg.remove(0));
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int i = 0; i < bucketNum; i++) {
            int offset = i * 2;
            int count = (bitmap.get(offset) ? 2 : 0) + (bitmap.get(offset + 1) ? 1 : 0);
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
        BitVector bitmap = BitVectorFactory.create(2 * bucketNum, partMsg.remove(0));
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int i = 0; i < bucketNum; i++) {
            int offset = i * 2;
            int count = (bitmap.get(offset) ? 2 : 0) + (bitmap.get(offset + 1) ? 1 : 0);fingerprints[i] = new byte[count * fingerprintByteLength];
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
        int bitmapSize = 2 * saveBucketNum;
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        List<byte[]> cuckooFilterList = new LinkedList<>();
        for (int i = fromBucketIndex, indicatorIndex = 0; i < toBucketIndex; i++) {
            int count = 0;
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                if (fingerprint != 0L) {
                    count++;
                    cuckooFilterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
                } else {
                    break;
                }
            }
            if (count >= 2) {
                bitmap.set(indicatorIndex, true);
            }
            if ((count & 1) == 1) {
                bitmap.set(indicatorIndex + 1, true);
            }
            indicatorIndex += 2;
        }
        byteArrayList.add(IntUtils.intToByteArray(saveBucketNum));
        byteArrayList.add(bitmap.getBytes());
        byteArrayList.add(SerializeUtils.compressEqual(cuckooFilterList, fingerprintByteLength));
        return byteArrayList;
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}

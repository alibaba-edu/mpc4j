package edu.alibaba.mpc4j.common.structure.fastfilter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Fast Cuckoo Filter test.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
@RunWith(Parameterized.class)
public class FastCuckooFilterTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 18;
    /**
     * split number of filter
     */
    private static final int SPLIT_NUM = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (FastCuckooFilterType type : FastCuckooFilterType.values()) {
            configurations.add(new Object[]{type.name(), type,});
        }

        return configurations;
    }

    private final FastCuckooFilterType type;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public FastCuckooFilterTest(String name, FastCuckooFilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testParameters() {
        testParameters(1);
        testParameters(2);
        for (int logN : new int[]{8, 12, 16, 20, 24}) {
            testParameters((1 << logN) - 1);
            testParameters(1 << logN);
            testParameters((1 << logN) + 1);
        }

    }

    private void testParameters(int maxSize) {
        FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, maxSize, secureRandom.nextLong());
        // type
        Assert.assertEquals(type, cuckooFilter.getType());
        // bucket num
        Assert.assertEquals(FastCuckooFilterFactory.getBucketNum(type, maxSize), cuckooFilter.getBucketNum());
        // entries per bucket
        Assert.assertEquals(FastCuckooFilterFactory.getEntriesPerBucket(type), cuckooFilter.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(FastCuckooFilterFactory.getFingerprintByteLength(type), cuckooFilter.getFingerprintByteLength());
    }

    @Test
    public void testCuckooFilter() {
        testCuckooFilter(1);
        testCuckooFilter(2);
        testCuckooFilter(1 << 8);
        testCuckooFilter(1 << 12);
    }

    private void testCuckooFilter(int maxSize) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            long hashSeed = secureRandom.nextLong();
            FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, maxSize, hashSeed);
            int bucketNum = cuckooFilter.getBucketNum();
            // start with empty filer
            Assert.assertEquals(0, cuckooFilter.size());
            // insert elements into the filter
            ArrayList<ByteBuffer> items = randomItems(maxSize);
            // trace modify buckets
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                long[][] copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> LongUtils.clone(cuckooFilter.getBucket(bucketIndex)))
                    .toArray(long[][]::new);
                // add an item
                TIntSet indexSet = cuckooFilter.modifyPut(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (!indexSet.contains(bucketIndex)) {
                        Assert.assertArrayEquals(copyBuckets[bucketIndex], cuckooFilter.getBucket(bucketIndex));
                    }
                }
            }
            // test estimate size
            long byteSize = cuckooFilter.byteSize();
            Assert.assertEquals(FastCuckooFilterFactory.estimateByteSize(type, maxSize), byteSize);
            // now start to remove bucket
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                long[][] copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> LongUtils.clone(cuckooFilter.getBucket(bucketIndex)))
                    .toArray(long[][]::new);
                // remove an item
                int removeBucketIndex = cuckooFilter.modifyRemove(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (bucketIndex == removeBucketIndex) {
                        Assert.assertFalse(Arrays.equals(copyBuckets[bucketIndex], cuckooFilter.getBucket(bucketIndex)));
                    } else {
                        Assert.assertArrayEquals(copyBuckets[bucketIndex], cuckooFilter.getBucket(bucketIndex));
                    }
                }
            }
        }
    }

    @Test
    public void testCuckooFilterPosition() {
        testCuckooFilterPosition(1);
        testCuckooFilterPosition(2);
        testCuckooFilterPosition(1 << 8);
        testCuckooFilterPosition(1 << 12);
    }

    private void testCuckooFilterPosition(int maxSize) {
        long hashSeed = secureRandom.nextLong();
        FastCuckooFilterPosition<ByteBuffer> parameterPosition = FastCuckooFilterFactory.createCuckooFilterPosition(type, maxSize, hashSeed);
        // type
        Assert.assertEquals(type, parameterPosition.getType());
        // bucket num
        Assert.assertEquals(FastCuckooFilterFactory.getBucketNum(type, maxSize), parameterPosition.getBucketNum());
        // entries per bucket
        Assert.assertEquals(FastCuckooFilterFactory.getEntriesPerBucket(type), parameterPosition.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(FastCuckooFilterFactory.getFingerprintByteLength(type), parameterPosition.getFingerprintByteLength());
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            hashSeed = secureRandom.nextLong();
            // create cuckoo filter and its corresponding cuckoo filter position.
            FastCuckooFilterPosition<ByteBuffer> cuckooFilterPosition = FastCuckooFilterFactory.createCuckooFilterPosition(type, maxSize, hashSeed);
            FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, maxSize, hashSeed);

            // insert elements into the filter
            ArrayList<ByteBuffer> items = randomItems(maxSize);
            for (int itemIndex = 0; itemIndex < maxSize; itemIndex++) {
                cuckooFilter.put(items.get(itemIndex));
            }
            // test each item
            for (ByteBuffer item : items) {
                Assert.assertTrue(cuckooFilter.mightContain(item));
                long fingerprint = cuckooFilterPosition.fingerprint(item);
                int[] positions = cuckooFilterPosition.positions(item);
                SingleTable table = cuckooFilter.getTable();
                Assert.assertTrue(table.findTagInBuckets(positions[0], positions[1], fingerprint));
            }
        }
    }

    @Test
    public void testSerialize() {
        long hashSeed = secureRandom.nextLong();
        FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, DEFAULT_SIZE, hashSeed);
        // insert elements into the filter
        ArrayList<ByteBuffer> items = randomItems(DEFAULT_SIZE);
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            cuckooFilter.put(items.get(i));
        }
        Assert.assertEquals(items.size(), cuckooFilter.size());
        // convert to byte array list
        List<byte[]> byteArrayList = cuckooFilter.save();
        FastCuckooFilter<ByteBuffer> recoveredRandomCuckooFilter = FastCuckooFilterFactory.loadCuckooFilter(byteArrayList);
        Assert.assertEquals(cuckooFilter, recoveredRandomCuckooFilter);
    }

    @Test
    public void testPartSerialize() {
        long hashSeed = secureRandom.nextLong();
        FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, LARGE_SIZE, hashSeed);
        // insert elements into the filter
        ArrayList<ByteBuffer> items = randomItems(LARGE_SIZE);
        for (int i = 0; i < LARGE_SIZE; i++) {
            cuckooFilter.put(items.get(i));
        }
        Assert.assertEquals(items.size(), cuckooFilter.size());
        // convert to multiple byte array list
        List<List<byte[]>> lists = new LinkedList<>();
        int startIndex = 0;
        int splitCount = 0;
        while (splitCount < SPLIT_NUM && startIndex < cuckooFilter.getBucketNum()) {
            splitCount++;
            int endIndex = splitCount == SPLIT_NUM ? cuckooFilter.getBucketNum() : secureRandom.nextInt(startIndex + 1, cuckooFilter.getBucketNum() + 1);
            lists.add(cuckooFilter.savePart(startIndex, endIndex));
            startIndex = endIndex;
        }
        // load part
        long[][] loadPartRes = lists.stream()
            .map(ea -> FastCuckooFilterFactory.loadPart(type, ea))
            .flatMap(Arrays::stream)
            .toArray(long[][]::new);
        Assert.assertEquals(loadPartRes.length, cuckooFilter.getBucketNum());
        for (int i = 0; i < loadPartRes.length; i++) {
            Assert.assertArrayEquals(loadPartRes[i], cuckooFilter.getBucket(i));
        }
    }

    @Test
    public void testPartByteLoad() {
        long hashSeed = secureRandom.nextLong();
        FastCuckooFilter<ByteBuffer> cuckooFilter = FastCuckooFilterFactory.createCuckooFilter(type, LARGE_SIZE, hashSeed);
        // insert elements into the filter
        ArrayList<ByteBuffer> items = randomItems(LARGE_SIZE);
        for (int i = 0; i < LARGE_SIZE; i++) {
            cuckooFilter.put(items.get(i));
        }
        Assert.assertEquals(items.size(), cuckooFilter.size());
        // convert to multiple byte array list
        List<List<byte[]>> lists = new LinkedList<>();
        int startIndex = 0;
        int splitCount = 0;
        while (splitCount < SPLIT_NUM && startIndex < cuckooFilter.getBucketNum()) {
            splitCount++;
            int endIndex = splitCount == SPLIT_NUM ? cuckooFilter.getBucketNum() : secureRandom.nextInt(startIndex + 1, cuckooFilter.getBucketNum() + 1);
            lists.add(cuckooFilter.savePart(startIndex, endIndex));
            startIndex = endIndex;
        }
        // load part byte
        byte[][] loadPartByteRes = lists.stream()
            .map(ea -> FastCuckooFilterFactory.loadPartByte(type, ea))
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        Assert.assertEquals(loadPartByteRes.length, cuckooFilter.getBucketNum());
        for (int i = 0; i < loadPartByteRes.length; i++) {
            long[] recoverLong = FastCuckooFilterFactory.recoverFingerprint(type, loadPartByteRes[i]);
            long[] original = cuckooFilter.getBucket(i);
            assert recoverLong == null || original.length >= recoverLong.length;
            int index = 0;
            int minLen = recoverLong == null ? 0 : recoverLong.length;
            for(; index < minLen; index++){
                assert original[index] == recoverLong[index];
            }
            for(; index < original.length; index++){
                assert original[index] == 0;
            }
        }
    }

    private ArrayList<ByteBuffer> randomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(i -> BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}

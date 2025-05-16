package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cuckoo Filter test.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
@RunWith(Parameterized.class)
public class CuckooFilterTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        for (CuckooFilterType cuckooFilterType : CuckooFilterType.values()) {
            configurationParams.add(new Object[]{cuckooFilterType.name(), cuckooFilterType,});
        }

        return configurationParams;
    }

    private final CuckooFilterType type;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public CuckooFilterTest(String name, CuckooFilterType type) {
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
        byte[][] keys = BlockUtils.randomBlocks(CuckooFilter.getHashKeyNum(), secureRandom);
        CuckooFilter<ByteBuffer> cuckooFilter = CuckooFilterFactory.createCuckooFilter(EnvType.STANDARD, type, maxSize, keys);
        // type
        Assert.assertEquals(type, cuckooFilter.getCuckooFilterType());
        // bucket num
        Assert.assertEquals(CuckooFilterFactory.getBucketNum(type, maxSize), cuckooFilter.getBucketNum());
        // entries per bucket
        Assert.assertEquals(CuckooFilterFactory.getEntriesPerBucket(type), cuckooFilter.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(CuckooFilterFactory.getFingerprintByteLength(type), cuckooFilter.getFingerprintByteLength());
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
            byte[][] keys = BlockUtils.randomBlocks(CuckooFilter.getHashKeyNum(), secureRandom);
            CuckooFilter<ByteBuffer> cuckooFilter = CuckooFilterFactory.createCuckooFilter(EnvType.STANDARD, type, maxSize, keys);
            int bucketNum = cuckooFilter.getBucketNum();
            // start with empty filer
            Assert.assertEquals(0, cuckooFilter.size());
            // insert elements into the filter
            ArrayList<ByteBuffer> items = randomItems(maxSize);
            // trace modify buckets
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                ArrayList<?> copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> new ArrayList<>(cuckooFilter.getBucket(bucketIndex)))
                    .collect(Collectors.toCollection(ArrayList::new));
                // add an item
                TIntSet indexSet = cuckooFilter.modifyPut(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (indexSet.contains(bucketIndex)) {
                        Assert.assertNotEquals(copyBuckets.get(bucketIndex), cuckooFilter.getBucket(bucketIndex));
                    } else {
                        Assert.assertEquals(copyBuckets.get(bucketIndex), cuckooFilter.getBucket(bucketIndex));
                    }
                }
            }
            // test estimate size
            long byteSize = cuckooFilter.byteSize();
            Assert.assertEquals(CuckooFilterFactory.estimateByteSize(type, maxSize), byteSize);
            // now start to remove bucket
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                ArrayList<ArrayList<ByteBuffer>> copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> new ArrayList<>(cuckooFilter.getBucket(bucketIndex)))
                    .collect(Collectors.toCollection(ArrayList::new));
                // remove an item
                int removeBucketIndex = cuckooFilter.modifyRemove(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (bucketIndex == removeBucketIndex) {
                        Assert.assertNotEquals(copyBuckets.get(bucketIndex), cuckooFilter.getBucket(bucketIndex));
                    } else {
                        Assert.assertEquals(copyBuckets.get(bucketIndex), cuckooFilter.getBucket(bucketIndex));
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
        // test parameters
        byte[][] parameterKeys = BlockUtils.randomBlocks(CuckooFilter.getHashKeyNum(), secureRandom);
        CuckooFilterPosition<ByteBuffer> parameterCuckooFilterPosition = CuckooFilterFactory
            .createCuckooFilterPosition(EnvType.STANDARD, type, maxSize, parameterKeys);
        // type
        Assert.assertEquals(type, parameterCuckooFilterPosition.getCuckooFilterType());
        // bucket num
        Assert.assertEquals(CuckooFilterFactory.getBucketNum(type, maxSize), parameterCuckooFilterPosition.getBucketNum());
        // entries per bucket
        Assert.assertEquals(CuckooFilterFactory.getEntriesPerBucket(type), parameterCuckooFilterPosition.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(CuckooFilterFactory.getFingerprintByteLength(type), parameterCuckooFilterPosition.getFingerprintByteLength());
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[][] keys = BlockUtils.randomBlocks(CuckooFilter.getHashKeyNum(), secureRandom);
            // create cuckoo filter and its corresponding cuckoo filter position.
            CuckooFilterPosition<ByteBuffer> cuckooFilterPosition = CuckooFilterFactory
                .createCuckooFilterPosition(EnvType.STANDARD, type, maxSize, keys);
            CuckooFilter<ByteBuffer> cuckooFilter = CuckooFilterFactory
                .createCuckooFilter(EnvType.STANDARD, type, maxSize, keys);

            // insert elements into the filter
            ArrayList<ByteBuffer> items = randomItems(maxSize);
            items.forEach(cuckooFilter::put);
            // test each item
            items.forEach(item -> {
                Assert.assertTrue(cuckooFilter.mightContain(item));
                ByteBuffer fingerprint = cuckooFilterPosition.fingerprint(item);
                int[] positions = cuckooFilterPosition.positions(item);
                ArrayList<ByteBuffer> bucket0 = cuckooFilter.getBucket(positions[0]);
                ArrayList<ByteBuffer> bucket1 = cuckooFilter.getBucket(positions[1]);
                boolean contain0 = bucket0.contains(fingerprint);
                boolean contain1 = bucket1.contains(fingerprint);
                if (positions[0] == positions[1]) {
                    // when we have same bucket indexes, then both contains return true
                    Assert.assertTrue(contain0 & contain1);
                } else {
                    // only one bucket contains the fingerprint
                    Assert.assertTrue(contain0 ^ contain1);
                }
            });
        }
    }

    @Test
    public void testSerialize() {
        byte[][] keys = BlockUtils.randomBlocks(CuckooFilter.getHashKeyNum(), secureRandom);
        CuckooFilter<ByteBuffer> cuckooFilter = CuckooFilterFactory.createCuckooFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
        // insert elements into the filter
        List<ByteBuffer> items = randomItems(DEFAULT_SIZE);
        items.forEach(cuckooFilter::put);
        Assert.assertEquals(items.size(), cuckooFilter.size());
        // convert to byte array list
        List<byte[]> byteArrayList = cuckooFilter.save();
        CuckooFilter<ByteBuffer> recoveredCuckooFilter = CuckooFilterFactory.loadCuckooFilter(EnvType.STANDARD, byteArrayList);
        Assert.assertEquals(cuckooFilter, recoveredCuckooFilter);
    }

    private ArrayList<ByteBuffer> randomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> {
                byte[] itemByteArray = BlockUtils.randomBlock(secureRandom);
                return ByteBuffer.wrap(itemByteArray);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }
}

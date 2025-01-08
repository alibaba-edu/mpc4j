package edu.alibaba.mpc4j.common.structure.rcfilter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
 * @date 2024/11/6
 */
@RunWith(Parameterized.class)
public class RandomCuckooFilterTest {
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

        for (RandomCuckooFilterType type : RandomCuckooFilterType.values()) {
            configurationParams.add(new Object[]{type.name(), type,});
        }

        return configurationParams;
    }

    private final RandomCuckooFilterType type;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public RandomCuckooFilterTest(String name, RandomCuckooFilterType type) {
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
        RandomCuckooFilter randomCuckooFilter = RandomCuckooFilterFactory.createCuckooFilter(type, maxSize);
        // type
        Assert.assertEquals(type, randomCuckooFilter.getType());
        // bucket num
        Assert.assertEquals(RandomCuckooFilterFactory.getBucketNum(type, maxSize), randomCuckooFilter.getBucketNum());
        // entries per bucket
        Assert.assertEquals(RandomCuckooFilterFactory.getEntriesPerBucket(type), randomCuckooFilter.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(RandomCuckooFilterFactory.getFingerprintByteLength(type), randomCuckooFilter.getFingerprintByteLength());
    }

    @Test
    public void testRandomCuckooFilter() {
        testRandomCuckooFilter(1);
        testRandomCuckooFilter(2);
        testRandomCuckooFilter(1 << 8);
        testRandomCuckooFilter(1 << 12);
    }

    private void testRandomCuckooFilter(int maxSize) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            RandomCuckooFilter randomCuckooFilter = RandomCuckooFilterFactory.createCuckooFilter(type, maxSize);
            int bucketNum = randomCuckooFilter.getBucketNum();
            // start with empty filer
            Assert.assertEquals(0, randomCuckooFilter.size());
            // insert elements into the filter
            TLongArrayList items = randomItems(maxSize);
            // trace modify buckets
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                ArrayList<?> copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> new TLongArrayList(randomCuckooFilter.getBucket(bucketIndex)))
                    .collect(Collectors.toCollection(ArrayList::new));
                // add an item
                TIntSet indexSet = randomCuckooFilter.modifyPut(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (indexSet.contains(bucketIndex)) {
                        Assert.assertNotEquals(copyBuckets.get(bucketIndex), randomCuckooFilter.getBucket(bucketIndex));
                    } else {
                        Assert.assertEquals(copyBuckets.get(bucketIndex), randomCuckooFilter.getBucket(bucketIndex));
                    }
                }
            }
            // test estimate size
            long byteSize = randomCuckooFilter.byteSize();
            Assert.assertEquals(RandomCuckooFilterFactory.estimateByteSize(type, maxSize), byteSize);
            // now start to remove bucket
            for (int index = 0; index < maxSize; index++) {
                // copy each buckets
                ArrayList<TLongArrayList> copyBuckets = IntStream.range(0, bucketNum)
                    .mapToObj(bucketIndex -> new TLongArrayList(randomCuckooFilter.getBucket(bucketIndex)))
                    .collect(Collectors.toCollection(ArrayList::new));
                // remove an item
                int removeBucketIndex = randomCuckooFilter.modifyRemove(items.get(index));
                // check correct bucket change
                for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
                    if (bucketIndex == removeBucketIndex) {
                        Assert.assertNotEquals(copyBuckets.get(bucketIndex), randomCuckooFilter.getBucket(bucketIndex));
                    } else {
                        Assert.assertEquals(copyBuckets.get(bucketIndex), randomCuckooFilter.getBucket(bucketIndex));
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
        RandomCuckooFilterPosition parameterRandomCuckooFilterPosition = RandomCuckooFilterFactory.createCuckooFilterPosition(type, maxSize);
        // type
        Assert.assertEquals(type, parameterRandomCuckooFilterPosition.getType());
        // bucket num
        Assert.assertEquals(RandomCuckooFilterFactory.getBucketNum(type, maxSize), parameterRandomCuckooFilterPosition.getBucketNum());
        // entries per bucket
        Assert.assertEquals(RandomCuckooFilterFactory.getEntriesPerBucket(type), parameterRandomCuckooFilterPosition.getEntriesPerBucket());
        // fingerprint byte length
        Assert.assertEquals(RandomCuckooFilterFactory.getFingerprintByteLength(type), parameterRandomCuckooFilterPosition.getFingerprintByteLength());
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // create cuckoo filter and its corresponding cuckoo filter position.
            RandomCuckooFilterPosition cuckooFilterPosition = RandomCuckooFilterFactory.createCuckooFilterPosition(type, maxSize);
            RandomCuckooFilter cuckooFilter = RandomCuckooFilterFactory.createCuckooFilter(type, maxSize);

            // insert elements into the filter
            TLongArrayList items = randomItems(maxSize);
            for (int itemIndex = 0; itemIndex < maxSize; itemIndex++) {
                cuckooFilter.put(items.get(itemIndex));
            }
            // test each item
            for (long item : items.toArray()) {
                Assert.assertTrue(cuckooFilter.mightContain(item));
                long fingerprint = cuckooFilterPosition.fingerprint(item);
                int[] positions = cuckooFilterPosition.positions(item);
                TLongArrayList bucket0 = cuckooFilter.getBucket(positions[0]);
                TLongArrayList bucket1 = cuckooFilter.getBucket(positions[1]);
                boolean contain0 = bucket0.contains(fingerprint);
                boolean contain1 = bucket1.contains(fingerprint);
                if (positions[0] == positions[1]) {
                    // when we have same bucket indexes, then both contains return true
                    Assert.assertTrue(contain0 & contain1);
                } else {
                    // only one bucket contains the fingerprint
                    Assert.assertTrue(contain0 ^ contain1);
                }
            }
        }
    }

    @Test
    public void testSerialize() {
        RandomCuckooFilter randomCuckooFilter = RandomCuckooFilterFactory.createCuckooFilter(type, DEFAULT_SIZE);
        // insert elements into the filter
        TLongArrayList items = randomItems(DEFAULT_SIZE);
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            randomCuckooFilter.put(items.get(i));
        }
        Assert.assertEquals(items.size(), randomCuckooFilter.size());
        // convert to byte array list
        List<byte[]> byteArrayList = randomCuckooFilter.save();
        RandomCuckooFilter recoveredRandomCuckooFilter = RandomCuckooFilterFactory.loadCuckooFilter(byteArrayList);
        Assert.assertEquals(randomCuckooFilter, recoveredRandomCuckooFilter);
    }

    private TLongArrayList randomItems(int size) {
        TLongArrayList randomItems = new TLongArrayList(size);
        for (int i = 0; i < size; i++) {
            randomItems.add(secureRandom.nextLong());
        }
        return randomItems;
    }
}

package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * filter test.
 *
 * @author Weiran Liu
 * @date 2022/01/08
 */
@RunWith(Parameterized.class)
public class FilterTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;
    /**
     * default length
     */
    private static final int DEFAULT_LENGTH = 16;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // DISTINCT_BLOOM_FILTER
        configurationParams.add(new Object[]{FilterType.DISTINCT_BLOOM_FILTER.name(), FilterType.DISTINCT_BLOOM_FILTER,});
        // VACUUM_FILTER
        configurationParams.add(new Object[]{FilterType.VACUUM_FILTER.name(), FilterType.VACUUM_FILTER,});
        // CUCKOO_FILTER
        configurationParams.add(new Object[]{FilterType.CUCKOO_FILTER.name(), FilterType.CUCKOO_FILTER,});
        // SPARSE_BLOOM_FILTER
        configurationParams.add(new Object[]{FilterType.SPARSE_RANDOM_BLOOM_FILTER.name(), FilterType.SPARSE_RANDOM_BLOOM_FILTER,});
        // NAIVE_BLOOM_FILTER
        configurationParams.add(new Object[]{FilterType.NAIVE_RANDOM_BLOOM_FILTER.name(), FilterType.NAIVE_RANDOM_BLOOM_FILTER,});
        // SET_FILTER
        configurationParams.add(new Object[]{FilterType.SET_FILTER.name(), FilterType.SET_FILTER,});

        return configurationParams;
    }

    private final FilterType type;

    public FilterTest(String name, FilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // try less keys
        if (FilterFactory.getHashKeyNum(type) > 0) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type) - 1, SECURE_RANDOM);
                FilterFactory.load(EnvType.STANDARD, type, DEFAULT_SIZE, lessKeys);
            });
        }
        // try more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type) + 1, SECURE_RANDOM);
            FilterFactory.load(EnvType.STANDARD, type, DEFAULT_SIZE, moreKeys);
        });
        // create filter with 0 elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
            FilterFactory.load(EnvType.STANDARD, type, 0, keys);
        });
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
        // insert duplicated elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Filter<ByteBuffer> filter = FilterFactory.load(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
            filter.put(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
            filter.put(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
        });
        // insert more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Filter<ByteBuffer> filter = FilterFactory.load(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
            Set<ByteBuffer> items = generateRandomItems(DEFAULT_SIZE + 1);
            items.forEach(filter::put);
        });
    }

    @Test
    public void testFilter() {
        testFilter(1);
        testFilter(2);
        testFilter(1 << 8);
        testFilter(1 << 12);
        testFilter(1 << 16);
    }

    private void testFilter(int maxSize) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
            Filter<ByteBuffer> filter = FilterFactory.load(EnvType.STANDARD, type, maxSize, keys);
            // start with empty filer
            Assert.assertEquals(0, filter.size());
            // insert elements into the filter
            Set<ByteBuffer> items = generateRandomItems(maxSize);
            items.forEach(filter::put);
            Assert.assertEquals(items.size(), filter.size());
            // verify all elements are in the filter
            items.forEach(item -> Assert.assertTrue(filter.mightContain(item)));
            // verify other elements are not in the filter
            Set<ByteBuffer> randomItems = generateRandomItems(maxSize);
            randomItems.forEach(randomItem -> Assert.assertFalse(filter.mightContain(randomItem)));
        }
    }

    @Test
    public void testSerialize() {
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
        Filter<ByteBuffer> filter = FilterFactory.load(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
        // insert elements into the filter
        Set<ByteBuffer> items = generateRandomItems(DEFAULT_SIZE);
        items.forEach(filter::put);
        Assert.assertEquals(items.size(), filter.size());
        // convert to byte array list
        List<byte[]> byteArrayList = filter.save();
        Filter<ByteBuffer> recoveredFilter = FilterFactory.load(EnvType.STANDARD, byteArrayList);
        Assert.assertEquals(filter, recoveredFilter);
    }

    private Set<ByteBuffer> generateRandomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> {
                byte[] itemByteArray = new byte[DEFAULT_LENGTH];
                SECURE_RANDOM.nextBytes(itemByteArray);
                return ByteBuffer.wrap(itemByteArray);
            })
            .collect(Collectors.toSet());
    }
}

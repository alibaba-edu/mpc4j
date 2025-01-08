package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory.BloomFilterType;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bloom Filter test.
 *
 * @author Weiran Liu
 * @date 2022/01/08
 */
@RunWith(Parameterized.class)
public class BloomFilterTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 1 << 16;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (BloomFilterType bloomFilterType : BloomFilterType.values()) {
            configurations.add(new Object[] {bloomFilterType.name(), bloomFilterType,});
        }

        return configurations;
    }

    private final BloomFilterType type;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public BloomFilterTest(String name, BloomFilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // merge filters with different maxSize
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] key = CommonUtils.generateRandomKey(secureRandom);
            BloomFilter<ByteBuffer> masterMergeFilter = BloomFilterFactory.createBloomFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2, key
            );
            Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            masterMergeFilterItems.forEach(masterMergeFilter::put);
            BloomFilter<ByteBuffer> slaveMergeFilter = BloomFilterFactory.createBloomFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 + 1, key
            );
            Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            slaveMergeFilterItems.forEach(slaveMergeFilter::put);
            masterMergeFilter.merge(slaveMergeFilter);
        });
        // merge filters with sum of sizes greater than maxSize
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[] key = CommonUtils.generateRandomKey(secureRandom);
            BloomFilter<ByteBuffer> masterMergeFilter = BloomFilterFactory.createBloomFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 - 1, key
            );
            Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            masterMergeFilterItems.forEach(masterMergeFilter::put);
            BloomFilter<ByteBuffer> slaveMergeFilter = BloomFilterFactory.createBloomFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 - 1, key
            );
            Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            slaveMergeFilterItems.forEach(slaveMergeFilter::put);
            masterMergeFilter.merge(slaveMergeFilter);
        });
    }

    @Test
    public void testType() {
        byte[] key = CommonUtils.generateRandomKey(secureRandom);
        BloomFilter<ByteBuffer> bloomFilter = BloomFilterFactory.createBloomFilter(EnvType.STANDARD, type, DEFAULT_SIZE, key);
        Assert.assertEquals(type, bloomFilter.getBloomFilterType());
    }

    @Test
    public void testBloomFilter() {
        testBloomFilter(1);
        testBloomFilter(2);
        testBloomFilter(1 << 8);
        testBloomFilter(1 << 12);
        testBloomFilter(1 << 16);
    }

    private void testBloomFilter(int maxSize) {
        byte[] key = CommonUtils.generateRandomKey(secureRandom);
        BloomFilter<ByteBuffer> masterMergeFilter = BloomFilterFactory.createBloomFilter(
            EnvType.STANDARD, type, maxSize * 2, key
        );
        BloomFilter<ByteBuffer> slaveMergeFilter = BloomFilterFactory.createBloomFilter(
            EnvType.STANDARD, type, maxSize * 2, key
        );
        // insert elements into the master filter
        Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(maxSize);
        masterMergeFilterItems.forEach(masterMergeFilter::put);
        // insert elements into the slave filter
        Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(maxSize);
        slaveMergeFilterItems.forEach(masterMergeFilter::put);
        // merge
        masterMergeFilter.merge(slaveMergeFilter);
        // verify
        Set<ByteBuffer> containItems = new HashSet<>(masterMergeFilterItems.size() + slaveMergeFilterItems.size());
        containItems.addAll(masterMergeFilterItems);
        containItems.addAll(slaveMergeFilterItems);
        // all elements are in the merged filter
        containItems.forEach(item -> Assert.assertTrue(masterMergeFilter.mightContain(item)));
        // other elements are not in the merged filter
        Set<ByteBuffer> randomItems = generateRandomItems(maxSize * 2);
        randomItems.forEach(randomItem -> Assert.assertFalse(masterMergeFilter.mightContain(randomItem)));
    }

    @Test
    public void testDistinctBloomFilter() {
        if (type.equals(BloomFilterType.DISTINCT_BLOOM_FILTER)) {
            byte[] key = CommonUtils.generateRandomKey(secureRandom);
            BloomFilter<ByteBuffer> filter = BloomFilterFactory.createBloomFilter(EnvType.STANDARD, type, DEFAULT_SIZE, key);
            byte[] itemByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
                // generate random inputs and test if the hash index are distinct
                secureRandom.nextBytes(itemByteArray);
                ByteBuffer item = ByteBuffer.wrap(itemByteArray);
                int[] hashIndexes = filter.hashIndexes(item);
                long distinctIndexes = Arrays.stream(hashIndexes).distinct().count();
                Assert.assertEquals(hashIndexes.length, distinctIndexes);
            }
        }
    }

    private Set<ByteBuffer> generateRandomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> {
                byte[] itemByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(itemByteArray);
                return ByteBuffer.wrap(itemByteArray);
            })
            .collect(Collectors.toSet());
    }
}

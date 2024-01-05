package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * merge filter test.
 *
 * @author Weiran Liu
 * @date 2022/01/08
 */
@RunWith(Parameterized.class)
public class MergeFilterTest {
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DISTINCT_BLOOM_FILTER
        configurations.add(new Object[] {FilterType.DISTINCT_BLOOM_FILTER.name(), FilterType.DISTINCT_BLOOM_FILTER,});
        // SPARSE_BLOOM_FILTER
        configurations.add(new Object[] {FilterType.SPARSE_RANDOM_BLOOM_FILTER.name(), FilterType.SPARSE_RANDOM_BLOOM_FILTER,});
        // BLOOM_FILTER
        configurations.add(new Object[] {FilterType.NAIVE_RANDOM_BLOOM_FILTER.name(), FilterType.NAIVE_RANDOM_BLOOM_FILTER,});
        // SET_FILTER
        configurations.add(new Object[] {FilterType.SET_FILTER.name(), FilterType.SET_FILTER,});

        return configurations;
    }

    private final FilterType type;

    public MergeFilterTest(String name, FilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // merge filters with different maxSize
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
            MergeFilter<ByteBuffer> masterMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2, keys
            );
            Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            masterMergeFilterItems.forEach(masterMergeFilter::put);
            MergeFilter<ByteBuffer> slaveMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 + 1, keys
            );
            Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            slaveMergeFilterItems.forEach(slaveMergeFilter::put);
            masterMergeFilter.merge(slaveMergeFilter);
        });
        // merge filters with sum of sizes greater than maxSize
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
            MergeFilter<ByteBuffer> masterMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 - 1, keys
            );
            Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            masterMergeFilterItems.forEach(masterMergeFilter::put);
            MergeFilter<ByteBuffer> slaveMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, DEFAULT_SIZE * 2 - 1, keys
            );
            Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(DEFAULT_SIZE);
            slaveMergeFilterItems.forEach(slaveMergeFilter::put);
            masterMergeFilter.merge(slaveMergeFilter);
        });
    }

    @Test
    public void testMergeFilter() {
        testMergeFilter(1);
        testMergeFilter(2);
        testMergeFilter(1 << 8);
        testMergeFilter(1 << 12);
        testMergeFilter(1 << 16);
    }

    private void testMergeFilter(int maxSize) {
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashKeyNum(type), SECURE_RANDOM);
        MergeFilter<ByteBuffer> masterMergeFilter = FilterFactory.createMergeFilter(
            EnvType.STANDARD, type, maxSize * 2, keys
        );
        MergeFilter<ByteBuffer> slaveMergeFilter = FilterFactory.createMergeFilter(
            EnvType.STANDARD, type, maxSize * 2, keys
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

    private Set<ByteBuffer> generateRandomItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> {
                byte[] itemByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(itemByteArray);
                return ByteBuffer.wrap(itemByteArray);
            })
            .collect(Collectors.toSet());
    }
}

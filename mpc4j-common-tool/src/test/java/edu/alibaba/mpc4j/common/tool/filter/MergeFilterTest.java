package edu.alibaba.mpc4j.common.tool.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
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
 * 合并过滤器测试。
 *
 * @author Weiran Liu
 * @date 2022/01/08
 */
@RunWith(Parameterized.class)
public class MergeFilterTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * 默认插入元素数量
     */
    private static final int DEFAULT_SIZE = 1 << 8;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // SPARSE_BLOOM_FILTER
        configurationParams.add(new Object[] {FilterType.SPARSE_BLOOM_FILTER.name(), FilterType.SPARSE_BLOOM_FILTER,});
        // BLOOM_FILTER
        configurationParams.add(new Object[] {FilterType.BLOOM_FILTER.name(), FilterType.BLOOM_FILTER,});
        // SET_FILTER
        configurationParams.add(new Object[] {FilterType.SET_FILTER.name(), FilterType.SET_FILTER,});

        return configurationParams;
    }

    private final FilterType type;

    public MergeFilterTest(String name, FilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 尝试合并不同最大数量的过滤器
        try {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, DEFAULT_SIZE), SECURE_RANDOM);
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
            throw new IllegalStateException("ERROR: successfully merger filters with different max size");
        } catch (AssertionError ignored) {

        }
        // 合并后的总元素数量超过最大数量
        try {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, DEFAULT_SIZE), SECURE_RANDOM);
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
            throw new IllegalStateException("ERROR: successfully merger filters with total size > max size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testMergeFilter() {
        // 1个元素
        testMergeFilter(1);
        // 2个元素
        testMergeFilter(2);
        // 2^8个元素
        testMergeFilter(1 << 8);
        // 插入2^12个元素
        testMergeFilter(1 << 12);
        // 插入2^16个元素
        testMergeFilter(1 << 16);
    }

    private void testMergeFilter(int maxSize) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, maxSize * 2), SECURE_RANDOM);
            MergeFilter<ByteBuffer> masterMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, maxSize * 2, keys
            );
            MergeFilter<ByteBuffer> slaveMergeFilter = FilterFactory.createMergeFilter(
                EnvType.STANDARD, type, maxSize * 2, keys
            );
            // 向主过滤器插入元素
            Set<ByteBuffer> masterMergeFilterItems = generateRandomItems(maxSize);
            masterMergeFilterItems.forEach(masterMergeFilter::put);
            // 向从过滤器插入元素
            Set<ByteBuffer> slaveMergeFilterItems = generateRandomItems(maxSize);
            slaveMergeFilterItems.forEach(masterMergeFilter::put);
            // 合并过滤器
            masterMergeFilter.merge(slaveMergeFilter);
            // 创建总集合
            Set<ByteBuffer> containItems = new HashSet<>(masterMergeFilterItems.size() + slaveMergeFilterItems.size());
            containItems.addAll(masterMergeFilterItems);
            containItems.addAll(slaveMergeFilterItems);
            // 验证所有元素都应在合并过滤器中
            containItems.forEach(item -> Assert.assertTrue(masterMergeFilter.mightContain(item)));
            // 验证其他随机元素不在过滤器中
            Set<ByteBuffer> randomItems = generateRandomItems(maxSize * 2);
            randomItems.forEach(randomItem -> Assert.assertFalse(masterMergeFilter.mightContain(randomItem)));
        }
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

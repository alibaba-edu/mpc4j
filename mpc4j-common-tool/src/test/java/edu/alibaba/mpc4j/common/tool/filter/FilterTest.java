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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 过滤器测试。
 *
 * @author Weiran Liu
 * @date 2022/01/08
 */
@RunWith(Parameterized.class)
public class FilterTest {
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
        // VACUUM_FILTER
        configurationParams.add(new Object[] {FilterType.VACUUM_FILTER.name(), FilterType.VACUUM_FILTER,});
        // CUCKOO_FILTER
        configurationParams.add(new Object[] {FilterType.CUCKOO_FILTER.name(), FilterType.CUCKOO_FILTER,});
        // SPARSE_BLOOM_FILTER
        configurationParams.add(new Object[] {FilterType.SPARSE_BLOOM_FILTER.name(), FilterType.SPARSE_BLOOM_FILTER,});
        // BLOOM_FILTER
        configurationParams.add(new Object[] {FilterType.BLOOM_FILTER.name(), FilterType.BLOOM_FILTER,});
        // SET_FILTER
        configurationParams.add(new Object[] {FilterType.SET_FILTER.name(), FilterType.SET_FILTER,});

        return configurationParams;
    }

    private final FilterType type;

    public FilterTest(String name, FilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 过滤器密钥长度不正确
        if (FilterFactory.getHashNum(type, DEFAULT_SIZE) > 0) {
            try {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(
                    FilterFactory.getHashNum(type, DEFAULT_SIZE) - 1, SECURE_RANDOM
                );
                FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, lessKeys);
                throw new IllegalStateException("ERROR: successfully create Filter with less keys");
            } catch (AssertionError ignored) {

            }
            try {
                byte[][] moreKeys = CommonUtils.generateRandomKeys(
                    FilterFactory.getHashNum(type, DEFAULT_SIZE) + 1, SECURE_RANDOM
                );
                FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, moreKeys);
                throw new IllegalStateException("ERROR: successfully create Filter with more keys");
            } catch (AssertionError ignored) {

            }
        }
        // 尝试创建插入0个元素的过滤器
        try {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, 0), SECURE_RANDOM);
            FilterFactory.createFilter(EnvType.STANDARD, type, 0, keys);
            throw new IllegalStateException("ERROR: successfully create Filter with 0 maxSize");
        } catch (AssertionError ignored) {

        }
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, DEFAULT_SIZE), SECURE_RANDOM);
        // 尝试插入重复元素
        try {
            Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
            filter.put(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
            filter.put(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
                throw new IllegalStateException("ERROR: successfully insert duplicated item into Filter");
            } catch (IllegalArgumentException ignored) {

        }
        // 尝试插入多余最大数量的元素
        try {
            Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
            Set<ByteBuffer> items = generateRandomItems(DEFAULT_SIZE + 1);
            items.forEach(filter::put);
            throw new IllegalStateException("ERROR: successfully insert more items into Filter");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testFilter() {
        // 1个元素
        testFilter(1);
        // 2个元素
        testFilter(2);
        // 2^8个元素
        testFilter(1 << 8);
        // 插入2^12个元素
        testFilter(1 << 12);
        // 插入2^16个元素
        testFilter(1 << 16);
    }

    private void testFilter(int maxSize) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, maxSize), SECURE_RANDOM);
            Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, maxSize, keys);
            // 开始时元素数量为0
            Assert.assertEquals(0, filter.size());
            // 向过滤器插入元素
            Set<ByteBuffer> items = generateRandomItems(maxSize);
            items.forEach(filter::put);
            Assert.assertEquals(items.size(), filter.size());
            // 验证所有应插入的元素都在过滤器中
            items.forEach(item -> Assert.assertTrue(filter.mightContain(item)));
            // 验证其他随机元素不在过滤器中
            Set<ByteBuffer> randomItems = generateRandomItems(maxSize);
            randomItems.forEach(randomItem -> Assert.assertFalse(filter.mightContain(randomItem)));
        }
    }

    @Test
    public void testSerialize() {
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, DEFAULT_SIZE), SECURE_RANDOM);
        Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
        // 向过滤器插入元素
        Set<ByteBuffer> items = generateRandomItems(DEFAULT_SIZE);
        items.forEach(filter::put);
        Assert.assertEquals(items.size(), filter.size());
        // 转换成字节数组
        List<byte[]> byteArrayList = filter.toByteArrayList();
        Filter<ByteBuffer> recoveredFilter = FilterFactory.createFilter(EnvType.STANDARD, byteArrayList);
        Assert.assertEquals(filter, recoveredFilter);
    }

    @Test
    public void testParallel() {
        byte[][] keys = CommonUtils.generateRandomKeys(FilterFactory.getHashNum(type, DEFAULT_SIZE), SECURE_RANDOM);
        Filter<ByteBuffer> filter = FilterFactory.createFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
        // 并行插入元素
        Set<ByteBuffer> items = generateRandomItems(DEFAULT_SIZE);
        items.stream().parallel().forEach(filter::put);
        Assert.assertEquals(items.size(), filter.size());
        // 验证所有应插入的元素都在过滤器中
        items.forEach(item -> Assert.assertTrue(filter.mightContain(item)));
        // 并行验证其他随机元素不在过滤器中
        Set<ByteBuffer> randomItems = generateRandomItems(DEFAULT_SIZE);
        randomItems.stream().parallel().forEach(randomItem -> Assert.assertFalse(filter.mightContain(randomItem)));
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

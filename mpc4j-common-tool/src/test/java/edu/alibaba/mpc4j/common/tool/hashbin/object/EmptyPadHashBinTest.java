package edu.alibaba.mpc4j.common.tool.hashbin.object;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 填充空元素哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2021/12/23
 */
@RunWith(Parameterized.class)
public class EmptyPadHashBinTest {
    private static final ByteBuffer EMPTY_ITEM = ByteBuffer.wrap(BlockUtils.zeroBlock());
    /**
     * 随机元素数量
     */
    private static final int RANDOM_ITEM_NUM = 400;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();

        configuration.add(new Object[] {"1 bin, 1 item", 1, 1, });

        configuration.add(new Object[] {"1 bin, 2 items", 1, 2, });
        configuration.add(new Object[] {"2 bin, 2 items", 2, 2, });

        configuration.add(new Object[] {"1 bin, 3 items", 1, 3,});
        configuration.add(new Object[] {"2 bin, 3 items", 2, 3,});
        configuration.add(new Object[] {"3 bin, 3 items", 3, 3,});

        configuration.add(new Object[] {"1 bin, 40 items", 1, CommonConstants.STATS_BIT_LENGTH, });
        configuration.add(new Object[] {"2 bin, 40 items", 1, CommonConstants.STATS_BIT_LENGTH, });
        configuration.add(new Object[] {"10 bin, 40 items", 10, CommonConstants.STATS_BIT_LENGTH, });

        configuration.add(new Object[] {"1 bin, 2^12 items", 1, 1 << 12, });
        configuration.add(new Object[] {"2 bin, 2^12 items", 2, 1 << 12, });
        configuration.add(new Object[] {"10 bin, 2^12 items", 10, 1 << 12, });
        configuration.add(new Object[] {"2^8 bin, 2^12 items", 1 << 8, 1 << 12, });

        configuration.add(new Object[] {"1 bin, 2^16 item", 1, 1 << 16, });
        configuration.add(new Object[] {"2 bin, 2^16 item", 2, 1 << 16, });
        configuration.add(new Object[] {"10 bin, 2^16 item", 10, 1 << 16, });
        configuration.add(new Object[] {"2^12 bin, 2^16 item", 1 << 12, 1 << 16, });

        return configuration;
    }

    /**
     * 哈希桶数量
     */
    private final int binNum;
    /**
     * 插入元素数量
     */
    private final int itemSize;

    public EmptyPadHashBinTest(String name, int binNum, int itemSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.binNum = binNum;
        this.itemSize = itemSize;
    }

    @Test
    public void testIllegalInputs() {
        byte[][] keys = BlockUtils.randomBlocks(1, HashBinTestUtils.SECURE_RANDOM);
        EmptyPadHashBin<ByteBuffer> hashBin = new EmptyPadHashBin<>(EnvType.STANDARD, binNum, itemSize, keys);
        List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(itemSize);
        // 尝试未插入元素时就填充数据
        try {
            hashBin.insertPaddingItems(EMPTY_ITEM);
            throw new IllegalStateException("ERROR: successfully insert padding items without inserting items");
        } catch (AssertionError ignored) {

        }
        // 尝试插入较多数量的元素
        try {
            List<ByteBuffer> moreItems = HashBinTestUtils.randomByteBufferItems(itemSize + 1);
            hashBin.insertItems(moreItems);
            throw new IllegalStateException("ERROR: successfully insert more items");
        } catch (AssertionError ignored) {

        }
        // 尝试插入重复元素
        if (itemSize > 1) {
            try {
                List<ByteBuffer> duplicateItems = HashBinTestUtils.randomByteBufferItems(itemSize - 2);
                duplicateItems.add(EMPTY_ITEM);
                duplicateItems.add(EMPTY_ITEM);
                hashBin.insertItems(duplicateItems);
                throw new IllegalStateException("ERROR: successfully insert duplicate items");
            } catch (IllegalArgumentException ignored) {

            }
        }
        hashBin.insertItems(items);
        // 尝试再次插入元素
        try {
            hashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert items twice");
        } catch (AssertionError ignored) {

        }
        hashBin.insertPaddingItems(EMPTY_ITEM);
        // 尝试再次填充重复元素
        try {
            hashBin.insertPaddingItems(EMPTY_ITEM);
            throw new IllegalStateException("ERROR: successfully insert padding items twice");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void test1Hash() {
        testHashBin(1);
    }

    @Test
    public void test2Hash() {
        testHashBin(2);
    }

    @Test
    public void test3Hash() {
        testHashBin(3);
    }

    @Test
    public void test4Hash() {
        testHashBin(4);
    }

    @Test
    public void test5Hash() {
        testHashBin(5);
    }

    private void testHashBin(int hashNum) {
        byte[][] keys = BlockUtils.randomBlocks(hashNum, HashBinTestUtils.SECURE_RANDOM);
        EmptyPadHashBin<ByteBuffer> hashBin = new EmptyPadHashBin<>(EnvType.STANDARD, binNum, itemSize, keys);
        List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(itemSize);
        // 验证参数设置
        Assert.assertEquals(binNum, hashBin.binNum());
        Assert.assertEquals(itemSize, hashBin.maxItemSize());
        // 验证插入之前的状态
        assertEmptyHashBin(hashBin);
        // 插入元素
        hashBin.insertItems(items);
        assertInsertedHashBin(hashBin, hashNum, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入元素和空元素
        hashBin.insertItems(items);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, hashNum, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入较小数量的元素
        items.remove(0);
        hashBin.insertItems(items);
        assertInsertedHashBin(hashBin, hashNum, items);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, hashNum, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入0个元素
        List<ByteBuffer> emptyItem = new LinkedList<>();
        hashBin.insertItems(emptyItem);
        assertInsertedHashBin(hashBin, hashNum, emptyItem);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, hashNum, emptyItem);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
    }

    private void assertEmptyHashBin(EmptyPadHashBin<ByteBuffer> hashBin) {
        Assert.assertFalse(hashBin.insertedItems());
        Assert.assertFalse(hashBin.insertedPaddingItems());
        Assert.assertEquals(0, hashBin.itemSize());
        Assert.assertEquals(0, hashBin.paddingItemSize());
        Assert.assertEquals(0, hashBin.size());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < hashBin.binNum(); binIndex++) {
            Assert.assertEquals(0, hashBin.getBin(binIndex).size());
        }
    }

    private void assertInsertedHashBin(EmptyPadHashBin<ByteBuffer> hashBin, int hashNum, List<ByteBuffer> items) {
        // 验证插入之后的状态
        Assert.assertTrue(hashBin.insertedItems());
        Assert.assertFalse(hashBin.insertedPaddingItems());
        // 验证所有的元素都在桶中
        for (ByteBuffer item : items) {
            Assert.assertTrue(hashBin.contains(item));
        }
        // 生成随机元素，验证所有随机元素都不在桶中
        List<ByteBuffer> randomItems = HashBinTestUtils.randomByteBufferItems(RANDOM_ITEM_NUM);
        randomItems.forEach(randomItem -> Assert.assertFalse(hashBin.contains(randomItem)));
        // 验证元素数量
        Assert.assertEquals(items.size() * hashNum, hashBin.itemSize());
        Assert.assertEquals(0, hashBin.paddingItemSize());
        Assert.assertEquals(items.size() * hashNum, hashBin.size());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < hashBin.binNum(); binIndex++) {
            Assert.assertTrue(hashBin.getBin(binIndex).size() <= hashBin.maxBinSize());
        }
    }

    private void assertPaddingHashBin(EmptyPadHashBin<ByteBuffer> hashBin, int hashNum, List<ByteBuffer> items) {
        // 验证元素数量
        Assert.assertEquals(items.size() * hashNum, hashBin.itemSize());
        Assert.assertEquals(hashBin.binNum() * hashBin.maxBinSize(), hashBin.size());
        // 验证所有的元素都在桶中
        for (ByteBuffer item : items) {
            Assert.assertTrue(hashBin.contains(item));
        }
        // 验证空元素仍然不在哈希桶中
        Assert.assertFalse(hashBin.contains(EMPTY_ITEM));
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < hashBin.binNum(); binIndex++) {
            Assert.assertEquals(hashBin.maxBinSize(), hashBin.getBin(binIndex).size());
        }
    }
}

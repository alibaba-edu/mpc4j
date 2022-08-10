package edu.alibaba.mpc4j.common.tool.hashbin.object;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
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
 * 选择哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
@RunWith(Parameterized.class)
public class TwoChoiceHashBinTest {
    /**
     * 随机元素数量
     */
    private static final int RANDOM_ITEM_NUM = 400;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();

        configuration.add(new Object[] {"1 item", 1,});
        configuration.add(new Object[] {"2 items", 2,});
        configuration.add(new Object[] {"3 items", 3,});
        configuration.add(new Object[] {"40 items", CommonConstants.STATS_BIT_LENGTH,});
        configuration.add(new Object[] {"2^12 items", 1 << 12, });
        configuration.add(new Object[] {"2^16 items", 1 << 16, });

        return configuration;
    }

    /**
     * 插入元素数量
     */
    private final int itemSize;

    public TwoChoiceHashBinTest(String name, int itemSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.itemSize = itemSize;
    }

    @Test
    public void testIllegalInputs() {
        byte[][] keys = CommonUtils.generateRandomKeys(2, HashBinTestUtils.SECURE_RANDOM);
        TwoChoiceHashBin<ByteBuffer> hashBin = new TwoChoiceHashBin<>(EnvType.STANDARD, itemSize, keys[0], keys[1]);
        // 尝试未插入元素时就填充数据
        try {
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
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
                duplicateItems.add(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
                duplicateItems.add(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
                hashBin.insertItems(duplicateItems);
                throw new IllegalStateException("ERROR: successfully insert duplicate items");
            } catch (IllegalArgumentException ignored) {

            }
        }
        List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(itemSize);
        hashBin.insertItems(items);
        // 尝试再次插入元素
        try {
            hashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert items twice");
        } catch (AssertionError ignored) {

        }
        hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
        // 尝试再次填充重复元素
        try {
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully insert padding items twice");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testHashBin() {
        byte[][] keys = CommonUtils.generateRandomKeys(2, HashBinTestUtils.SECURE_RANDOM);
        TwoChoiceHashBin<ByteBuffer> hashBin = new TwoChoiceHashBin<>(EnvType.STANDARD, itemSize, keys[0], keys[1]);
        List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(itemSize);
        assertHashBinParams(hashBin);
        assertEmptyHashBin(hashBin);
        // 插入元素
        hashBin.insertItems(items);
        assertInsertedHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入元素和虚拟元素
        hashBin.insertItems(items);
        hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
        assertPaddingHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入较小数量的元素
        items.remove(0);
        hashBin.insertItems(items);
        hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
        assertPaddingHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入0个元素
        List<ByteBuffer> emptyItems = new LinkedList<>();
        hashBin.insertItems(emptyItems);
        hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
        assertPaddingHashBin(hashBin, emptyItems);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
    }

    private void assertHashBinParams(TwoChoiceHashBin<ByteBuffer> hashBin) {
        Assert.assertEquals(itemSize, hashBin.maxItemSize());
        Assert.assertEquals(TwoChoiceHashBin.expectedBinNum(itemSize), hashBin.binNum());
        Assert.assertEquals(TwoChoiceHashBin.expectedMaxBinSize(itemSize), hashBin.maxBinSize());
    }

    private void assertEmptyHashBin(TwoChoiceHashBin<ByteBuffer> hashBin) {
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

    private void assertInsertedHashBin(TwoChoiceHashBin<ByteBuffer> hashBin, List<ByteBuffer> items) {
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
        Assert.assertEquals(items.size(), hashBin.itemSize());
        Assert.assertEquals(0, hashBin.paddingItemSize());
        Assert.assertEquals(items.size(), hashBin.size());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < hashBin.binNum(); binIndex++) {
            Assert.assertTrue(hashBin.getBin(binIndex).size() <= hashBin.maxBinSize());
        }
    }

    private void assertPaddingHashBin(TwoChoiceHashBin<ByteBuffer> hashBin, List<ByteBuffer> items) {
        // 验证元素数量
        Assert.assertEquals(items.size(), hashBin.itemSize());
        Assert.assertEquals(hashBin.binNum() * hashBin.maxBinSize(), hashBin.size());
        // 验证所有的元素都在桶中
        for (ByteBuffer item : items) {
            Assert.assertTrue(hashBin.contains(item));
        }
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < hashBin.binNum(); binIndex++) {
            Assert.assertEquals(hashBin.maxBinSize(), hashBin.getBin(binIndex).size());
        }
    }
}

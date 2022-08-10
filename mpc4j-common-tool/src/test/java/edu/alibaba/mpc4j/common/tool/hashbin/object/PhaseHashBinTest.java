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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Phase哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2021/12/23
 */
@RunWith(Parameterized.class)
public class PhaseHashBinTest {
    private static final BigInteger EMPTY_ITEM = BigInteger.ZERO;
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
     * 插入元素数量
     */
    private final int itemSize;
    /**
     * 桶数量
     */
    private final int binNum;

    public PhaseHashBinTest(String name, int binNum, int itemSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.binNum = binNum;
        this.itemSize = itemSize;
    }

    @Test
    public void testIllegalInputs() {
        byte[] key = CommonUtils.generateRandomKey(HashBinTestUtils.SECURE_RANDOM);
        PhaseHashBin hashBin = new PhaseHashBin(EnvType.STANDARD, binNum, itemSize, key);
        // 尝试未插入元素时就填充数据
        try {
            hashBin.insertPaddingItems(EMPTY_ITEM);
            throw new IllegalStateException("ERROR: successfully insert padding items without inserting items");
        } catch (AssertionError ignored) {

        }
        // 尝试插入较多数量的元素
        try {
            List<BigInteger> moreItems = HashBinTestUtils.randomBigIntegerItems(itemSize + 1);
            hashBin.insertItems(moreItems);
            throw new IllegalStateException("ERROR: successfully insert more items");
        } catch (AssertionError ignored) {

        }
        // 尝试插入重复元素
        if (itemSize > 1) {
            try {
                List<BigInteger> duplicateItems = HashBinTestUtils.randomBigIntegerItems(itemSize - 2);
                duplicateItems.add(BigInteger.ZERO);
                duplicateItems.add(BigInteger.ZERO);
                hashBin.insertItems(duplicateItems);
                throw new IllegalStateException("ERROR: successfully insert duplicate items");
            } catch (IllegalArgumentException ignored) {

            }
        }
        List<BigInteger> items = HashBinTestUtils.randomBigIntegerItems(itemSize);
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
    public void testPhaseHashBin() {
        byte[] key = CommonUtils.generateRandomKey(HashBinTestUtils.SECURE_RANDOM);
        PhaseHashBin hashBin = new PhaseHashBin(EnvType.STANDARD, binNum, itemSize, key);
        List<BigInteger> items = HashBinTestUtils.randomBigIntegerItems(itemSize);
        Assert.assertEquals(binNum, hashBin.binNum());
        Assert.assertEquals(itemSize, hashBin.maxItemSize());
        // 验证插入之前的状态
        assertEmptyHashBin(hashBin);
        // 插入元素
        hashBin.insertItems(items);
        assertInsertedHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入元素和空元素
        hashBin.insertItems(items);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入较小数量的元素
        items.remove(0);
        hashBin.insertItems(items);
        assertInsertedHashBin(hashBin, items);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, items);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
        // 插入0个元素
        List<BigInteger> emptyItems = new LinkedList<>();
        hashBin.insertItems(emptyItems);
        assertInsertedHashBin(hashBin, emptyItems);
        hashBin.insertPaddingItems(EMPTY_ITEM);
        assertPaddingHashBin(hashBin, emptyItems);
        hashBin.clear();
        assertEmptyHashBin(hashBin);
    }

    private void assertEmptyHashBin(PhaseHashBin phaseHashBin) {
        Assert.assertFalse(phaseHashBin.insertedItems());
        Assert.assertFalse(phaseHashBin.insertedPaddingItems());
        Assert.assertEquals(0, phaseHashBin.itemSize());
        Assert.assertEquals(0, phaseHashBin.paddingItemSize());
        Assert.assertEquals(0, phaseHashBin.size());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < phaseHashBin.binNum(); binIndex++) {
            Assert.assertEquals(0, phaseHashBin.getBin(binIndex).size());
        }
    }

    private void assertInsertedHashBin(PhaseHashBin phaseHashBin, List<BigInteger> items) {
        // 验证插入之后的状态
        Assert.assertTrue(phaseHashBin.insertedItems());
        Assert.assertFalse(phaseHashBin.insertedPaddingItems());
        // 验证所有的元素都在桶中
        for (BigInteger item : items) {
            Assert.assertTrue(phaseHashBin.contains(item));
        }
        // 生成随机元素，验证所有随机元素都不在桶中
        List<BigInteger> randomItems = HashBinTestUtils.randomBigIntegerItems(RANDOM_ITEM_NUM);
        randomItems.forEach(randomItem -> Assert.assertFalse(phaseHashBin.contains(randomItem)));
        // 验证元素数量
        Assert.assertEquals(items.size(), phaseHashBin.itemSize());
        Assert.assertEquals(0, phaseHashBin.paddingItemSize());
        Assert.assertEquals(items.size(), phaseHashBin.size());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < phaseHashBin.binNum(); binIndex++) {
            Assert.assertTrue(phaseHashBin.getBin(binIndex).size() <= phaseHashBin.maxBinSize());
        }
    }

    private void assertPaddingHashBin(PhaseHashBin phaseHashBin, List<BigInteger> items) {
        // 验证元素数量
        Assert.assertEquals(items.size(), phaseHashBin.itemSize());
        Assert.assertEquals(phaseHashBin.binNum() * phaseHashBin.maxBinSize(), phaseHashBin.size());
        // 验证所有的元素都在桶中
        for (BigInteger item : items) {
            Assert.assertTrue(phaseHashBin.contains(item));
        }
        // 验证空元素仍然不在哈希桶中
        Assert.assertFalse(phaseHashBin.contains(EMPTY_ITEM));
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < phaseHashBin.binNum(); binIndex++) {
            Assert.assertEquals(phaseHashBin.maxBinSize(), phaseHashBin.getBin(binIndex).size());
        }
    }
}

package edu.alibaba.mpc4j.common.tool.hashbin.primitive;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 简单哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
@RunWith(Parameterized.class)
public class SimpleIntHashBinTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[] {"1 bin, 1 item", 1, 1, });

        configurations.add(new Object[] {"1 bin, 2 items", 1, 2, });
        configurations.add(new Object[] {"2 bin, 2 items", 2, 2, });

        configurations.add(new Object[] {"1 bin, 3 items", 1, 3,});
        configurations.add(new Object[] {"2 bin, 3 items", 2, 3,});
        configurations.add(new Object[] {"3 bin, 3 items", 3, 3,});

        configurations.add(new Object[] {"1 bin, 40 items", 1, CommonConstants.STATS_BIT_LENGTH, });
        configurations.add(new Object[] {"2 bin, 40 items", 1, CommonConstants.STATS_BIT_LENGTH, });
        configurations.add(new Object[] {"10 bin, 40 items", 10, CommonConstants.STATS_BIT_LENGTH, });

        configurations.add(new Object[] {"1 bin, 2^12 items", 1, 1 << 12, });
        configurations.add(new Object[] {"2 bin, 2^12 items", 2, 1 << 12, });
        configurations.add(new Object[] {"10 bin, 2^12 items", 10, 1 << 12, });
        configurations.add(new Object[] {"2^8 bin, 2^12 items", 1 << 8, 1 << 12, });

        configurations.add(new Object[] {"1 bin, 2^16 item", 1, 1 << 16, });
        configurations.add(new Object[] {"2 bin, 2^16 item", 2, 1 << 16, });
        configurations.add(new Object[] {"10 bin, 2^16 item", 10, 1 << 16, });
        configurations.add(new Object[] {"2^12 bin, 2^16 item", 1 << 12, 1 << 16, });

        return configurations;
    }

    /**
     * 哈希桶数量
     */
    private final int binNum;
    /**
     * 插入元素数量
     */
    private final int itemSize;

    public SimpleIntHashBinTest(String name, int binNum, int itemSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.binNum = binNum;
        this.itemSize = itemSize;
    }

    @Test
    public void testIllegalInputs() {
        byte[][] keys = CommonUtils.generateRandomKeys(1, HashBinTestUtils.SECURE_RANDOM);
        SimpleIntHashBin intHashBin = new SimpleIntHashBin(EnvType.STANDARD, binNum, itemSize, keys);
        // 尝试插入较多数量的元素
        try {
            int[] items = randomItems(itemSize + 1);
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert more items into IntCuckooHashBin");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试插入负数元素
            int[] items = IntStream.range(-1, itemSize - 1).toArray();
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert negative item");
        } catch (AssertionError ignored) {

        }
        int[] items = randomItems(itemSize);
        intHashBin.insertItems(items);
        // 尝试再次插入元素
        try {
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert items twice");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void test1Hash() {
        testIntHashBin(1);
    }

    @Test
    public void test2Hash() {
        testIntHashBin(2);
    }

    @Test
    public void test3Hash() {
        testIntHashBin(3);
    }

    @Test
    public void test4Hash() {
        testIntHashBin(4);
    }

    @Test
    public void test5Hash() {
        testIntHashBin(5);
    }

    private void testIntHashBin(int hashNum) {
        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, HashBinTestUtils.SECURE_RANDOM);
        SimpleIntHashBin intHashBin = new SimpleIntHashBin(EnvType.STANDARD, binNum, itemSize, keys);
        // 验证参数设置
        Assert.assertEquals(hashNum, intHashBin.getHashNum());
        Assert.assertEquals(binNum, intHashBin.binNum());
        Assert.assertEquals(itemSize, intHashBin.maxItemSize());
        // 验证插入之前的状态
        assertEmptyIntHashBin(intHashBin);
        // 插入元素
        int[] items = randomItems(itemSize);
        intHashBin.insertItems(items);
        assertInsertedIntHashBin(intHashBin, hashNum, items);
        assertItemBinIndexes(intHashBin, keys, items);
        intHashBin.clear();
        assertEmptyIntHashBin(intHashBin);
        // 再次插入元素
        intHashBin.insertItems(items);
        assertInsertedIntHashBin(intHashBin, hashNum, items);
        assertItemBinIndexes(intHashBin, keys, items);
        intHashBin.clear();
        assertEmptyIntHashBin(intHashBin);
        // 插入较少数量的元素
        items = randomItems(itemSize - 1);
        intHashBin.insertItems(items);
        assertInsertedIntHashBin(intHashBin, hashNum, items);
        assertItemBinIndexes(intHashBin, keys, items);
        intHashBin.clear();
        assertEmptyIntHashBin(intHashBin);
        // 插入0个元素
        int[] emptyItems = new int[0];
        intHashBin.insertItems(emptyItems);
        assertInsertedIntHashBin(intHashBin, hashNum, emptyItems);
        assertItemBinIndexes(intHashBin, keys, emptyItems);
        intHashBin.clear();
        assertEmptyIntHashBin(intHashBin);
    }

    private void assertEmptyIntHashBin(SimpleIntHashBin intHashBin) {
        Assert.assertFalse(intHashBin.insertedItems());
        Assert.assertEquals(0, intHashBin.itemSize());
        // 验证每个桶的数量
        for (int binIndex = 0; binIndex < intHashBin.binNum(); binIndex++) {
            Assert.assertEquals(0, intHashBin.binSize(binIndex));
        }
    }

    private void assertInsertedIntHashBin(SimpleIntHashBin intHashBin, int hashNum, int[] items) {
        // 验证插入之后的状态
        Assert.assertTrue(intHashBin.insertedItems());
        // 验证元素数量
        Assert.assertEquals(items.length * hashNum, intHashBin.itemSize());
        // 验证每个桶的数量
        int size = IntStream.range(0, intHashBin.binNum())
            .map(intHashBin::binSize)
            .peek(binSize -> Assert.assertTrue(binSize <= intHashBin.maxBinSize()))
            .sum();
        Assert.assertEquals(items.length * hashNum, size);
        // 验证所有元素均在集合中
        for (int item : items) {
            Assert.assertTrue(intHashBin.contains(item));
        }
    }

    private void assertItemBinIndexes(SimpleIntHashBin intHashBin, byte[][] keys, int[] items) {
        // 外部初始化哈希函数，计算位置，验证外部计算的结果与内部计算结果相同
        Prf[] hashes = Arrays.stream(keys).map(key -> {
                Prf prf = PrfFactory.createInstance(EnvType.STANDARD, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        for (int item : items) {
            int[] itemBinIndexes = new int[keys.length];
            for (int hashIndex = 0; hashIndex < keys.length; hashIndex++) {
                itemBinIndexes[hashIndex] = hashes[hashIndex]
                    .getInteger(IntUtils.intToByteArray(item), intHashBin.binNum());
            }
            Assert.assertArrayEquals(itemBinIndexes, intHashBin.getItemBinIndexes(item));
        }
    }

    private int[] randomItems(int size) {
        TIntSet itemSet = new TIntHashSet(size);
        while (itemSet.size() < size) {
            itemSet.add(Math.abs(HashBinTestUtils.SECURE_RANDOM.nextInt()));
        }
        return itemSet.toArray();
    }
}

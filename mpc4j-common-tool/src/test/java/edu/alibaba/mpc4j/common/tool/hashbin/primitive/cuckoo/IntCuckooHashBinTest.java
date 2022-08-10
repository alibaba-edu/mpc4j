package edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 整数布谷鸟哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
@RunWith(Parameterized.class)
public class IntCuckooHashBinTest {
    /**
     * 随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 50;
    /**
     * 默认元素数量
     */
    private static final int DEFAULT_N = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // DRRT18
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_DRRT18.name(), IntCuckooHashBinType.NO_STASH_DRRT18
        });
        // PSZ18_5_HASH
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_PSZ18_5_HASH.name(), IntCuckooHashBinType.NO_STASH_PSZ18_5_HASH
        });
        // PSZ18_4_HASH
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_PSZ18_4_HASH.name(), IntCuckooHashBinType.NO_STASH_PSZ18_4_HASH
        });
        // PSZ18_3_HASH
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_PSZ18_3_HASH.name(), IntCuckooHashBinType.NO_STASH_PSZ18_3_HASH
        });
        // DRRT18
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_DRRT18.name(), IntCuckooHashBinType.NO_STASH_DRRT18
        });
        // NAIVE
        configurations.add(new Object[]{
            IntCuckooHashBinType.NO_STASH_NAIVE.name(), IntCuckooHashBinType.NO_STASH_NAIVE
        });

        return configurations;
    }

    /**
     * 布谷鸟哈希通类型
     */
    private final IntCuckooHashBinType type;

    public IntCuckooHashBinTest(String name, IntCuckooHashBinType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 密钥长度不正确
        try {
            byte[][] lessKeys = CommonUtils.generateRandomKeys(
                IntCuckooHashBinFactory.getHashNum(type) - 1, HashBinTestUtils.SECURE_RANDOM
            );
            IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, 1 << 16, lessKeys);
            throw new IllegalStateException("ERROR: successfully create IntCuckooHashBin with kess keys");
        } catch (AssertionError ignored) {

        }
        try {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(
                IntCuckooHashBinFactory.getHashNum(type) + 1, HashBinTestUtils.SECURE_RANDOM
            );
            IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, moreKeys);
            throw new IllegalStateException("ERROR: successfully create IntCuckooHashBin with more keys");
        } catch (AssertionError ignored) {

        }
        // 尝试创建插入0个元素的哈希桶
        try {
            byte[][] initKeys = CommonUtils.generateRandomKeys(
                IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
            );
            IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, 0, initKeys);
            throw new IllegalStateException("ERROR: successfully create IntCuckooHashBin with 0 maxSize");
        } catch (AssertionError ignored) {

        }
        byte[][] keys = CommonUtils.generateRandomKeys(
            IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
        );
        IntNoStashCuckooHashBin intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
        // 尝试插入较多数量的元素
        try {
            int[] items = HashBinTestUtils.randomIntItems(DEFAULT_N + 1);
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert more items into IntCuckooHashBin");
        } catch (AssertionError ignored) {

        }
        // 尝试插入重复元素
        int[] distinctItems = HashBinTestUtils.randomIntItems(DEFAULT_N - 2);
        int[] duplicateItems = Arrays.copyOf(distinctItems, DEFAULT_N);
        boolean duplicateSuccess = false;
        while (!duplicateSuccess) {
            try {
                intHashBin.insertItems(duplicateItems);
                //noinspection UnusedAssignment
                duplicateSuccess = true;
                throw new IllegalStateException("ERROR: successfully insert duplicated items into IntCuckooHashBin");
            } catch (ArithmeticException e) {
                keys = CommonUtils.generateRandomKeys(
                    IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                );
                intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
            } catch (IllegalArgumentException ignored) {
                break;
            }
        }
        try {
            // 尝试插入负数元素
            int[] items = IntStream.range(-1, DEFAULT_N - 1).toArray();
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert negative item");
        } catch (AssertionError ignored) {

        }
        // 插入元素
        int[] items = HashBinTestUtils.randomIntItems(DEFAULT_N);
        boolean success = false;
        while (!success) {
            try {
                intHashBin.insertItems(items);
                success = true;
            } catch (ArithmeticException ignored) {
                keys = CommonUtils.generateRandomKeys(
                    IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                );
                intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
            }
        }
        // 尝试再次插入元素
        try {
            intHashBin.insertItems(items);
            throw new IllegalStateException("ERROR: successfully insert items twice into IntCuckooHashBin");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(
            IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
        );
        IntNoStashCuckooHashBin intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, keys);
        Assert.assertEquals(type, intHashBin.getType());
    }

    @Test
    public void test1n() {
        testIntCuckooHashBin(1);
    }

    @Test
    public void test2n() {
        testIntCuckooHashBin(2);
    }

    @Test
    public void test3n() {
        testIntCuckooHashBin(3);
    }

    @Test
    public void test40n() {
        testIntCuckooHashBin(40);
    }

    @Test
    public void test256n() {
        testIntCuckooHashBin(256);
    }

    @Test
    public void test4096n() {
        testIntCuckooHashBin(4096);
    }

    private void testIntCuckooHashBin(int n) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            int[] items = HashBinTestUtils.randomIntItems(n);
            byte[][] keys = CommonUtils.generateRandomKeys(
                IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
            );
            IntNoStashCuckooHashBin intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, n, keys);
            // 验证插入前的状态
            assertEmptyIntHashBin(intHashBin);
            boolean success = false;
            while (!success) {
                try {
                    intHashBin.insertItems(items);
                    success = true;
                } catch (ArithmeticException ignored) {
                    keys = CommonUtils.generateRandomKeys(
                        IntCuckooHashBinFactory.getHashNum(type), HashBinTestUtils.SECURE_RANDOM
                    );
                    intHashBin = IntCuckooHashBinFactory.createInstance(EnvType.STANDARD, type, n, keys);
                }
            }
            assertInsertedIntHashBin(intHashBin, items);
            assertItemBinIndexes(intHashBin, keys, items);
            intHashBin.clear();
            assertEmptyIntHashBin(intHashBin);
            // 再次插入元素
            intHashBin.insertItems(items);
            assertInsertedIntHashBin(intHashBin, items);
            assertItemBinIndexes(intHashBin, keys, items);
            intHashBin.clear();
            assertEmptyIntHashBin(intHashBin);
            // 插入较小数量元素
            items = Arrays.copyOf(items, items.length - 1);
            intHashBin.insertItems(items);
            assertInsertedIntHashBin(intHashBin, items);
            assertItemBinIndexes(intHashBin, keys, items);
            intHashBin.clear();
            assertEmptyIntHashBin(intHashBin);
            // 插入0个元素
            int[] emptyItems = new int[0];
            intHashBin.insertItems(emptyItems);
            assertInsertedIntHashBin(intHashBin, emptyItems);
            assertItemBinIndexes(intHashBin, keys, emptyItems);
            intHashBin.clear();
            assertEmptyIntHashBin(intHashBin);
        }
    }

    private void assertEmptyIntHashBin(IntNoStashCuckooHashBin intHashBin) {
        // 验证状态
        Assert.assertFalse(intHashBin.insertedItems());
        // 验证数量
        Assert.assertEquals(0, intHashBin.itemSize());
    }

    private void assertInsertedIntHashBin(IntNoStashCuckooHashBin intHashBin, int[] items) {
        // 验证状态
        Assert.assertTrue(intHashBin.insertedItems());
        // 验证插入元素数量
        Assert.assertEquals(items.length, intHashBin.itemSize());
        // 并发验证所有的元素都在布谷鸟哈希桶中
        Arrays.stream(items).parallel().forEach(item -> Assert.assertTrue(intHashBin.contains(item)));
    }

    private void assertItemBinIndexes(IntNoStashCuckooHashBin intHashBin, byte[][] keys, int[] items) {
        // 外部初始化哈希函数，计算位置，验证外部计算的结果与内部计算结果相同
        Prf[] hashes = Arrays.stream(keys).map(key -> {
                Prf prf = PrfFactory.createInstance(EnvType.STANDARD, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        Arrays.stream(items).forEach(item -> {
            Set<Integer> itemBinIndexSet = IntStream.range(0, keys.length)
                .map(hashIndex -> hashes[hashIndex]
                    .getInteger(IntUtils.intToByteArray(item), intHashBin.binNum()))
                .map(intHashBin::getBinEntry)
                .boxed()
                .collect(Collectors.toSet());
            Assert.assertTrue(itemBinIndexSet.contains(item));
        });
    }
}

package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.HashBinTestUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 单哈希布谷鸟哈希桶测试。
 *
 * @author Weiran Liu
 * @date 2022/7/12
 */
public class OneHashCuckooHashBinTest {
    /**
     * 随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 50;
    /**
     * 默认桶数量
     */
    private static final int DEFAULT_BIN_NUM = 3;

    @Test
    public void testIllegalInputs() {
        // try less keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] lessKeys = CommonUtils.generateRandomKeys(
                CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH) - 1, HashBinTestUtils.SECURE_RANDOM
            );
            CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, DEFAULT_BIN_NUM, lessKeys
            );
        });
        // try more keys
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(
                CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH) + 1, HashBinTestUtils.SECURE_RANDOM
            );
            CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, DEFAULT_BIN_NUM, moreKeys
            );
        });
        final byte[][] keys = CommonUtils.generateRandomKeys(
            CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH), HashBinTestUtils.SECURE_RANDOM
        );
        // try 0 items
        Assert.assertThrows(IllegalArgumentException.class, () ->
            CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 0, DEFAULT_BIN_NUM, keys
            )
        );
        // try 2 items
        Assert.assertThrows(IllegalArgumentException.class, () ->
            CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 2, DEFAULT_BIN_NUM, keys
            )
        );
        // try creating without assigning bin num
        Assert.assertThrows(IllegalArgumentException.class, () ->
            CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, keys
            )
        );
        final CuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createCuckooHashBin(
            EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, DEFAULT_BIN_NUM, keys
        );
        // try inserting dummy items before inserting items
        Assert.assertThrows(IllegalArgumentException.class, () ->
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM)
        );
        // try inserting empty items before inserting iemtes
        Assert.assertThrows(IllegalArgumentException.class, () ->
            hashBin.insertPaddingItems(ByteBuffer.wrap(new byte[0]))
        );
        // try inserting more elements
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(2);
            hashBin.insertItems(items);
        });
        List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(1);
        hashBin.insertItems(items);
        // try inserting items twice
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            List<ByteBuffer> moreItems = HashBinTestUtils.randomByteBufferItems(1);
            hashBin.insertItems(moreItems);
        });
        // try padding existing items
        Assert.assertThrows(IllegalArgumentException.class, () ->
            hashBin.insertPaddingItems(items.get(0))
        );
        hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
        // try padding items twice
        Assert.assertThrows(IllegalArgumentException.class, () ->
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM)
        );
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(
            CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH), HashBinTestUtils.SECURE_RANDOM
        );
        CuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createCuckooHashBin(
            EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, DEFAULT_BIN_NUM, keys
        );
        Assert.assertEquals(CuckooHashBinType.NO_STASH_ONE_HASH, hashBin.getType());
    }

    @Test
    public void test1n() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[][] keys = CommonUtils.generateRandomKeys(
                CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH), HashBinTestUtils.SECURE_RANDOM
            );
            CuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createCuckooHashBin(
                EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, 3, keys
            );
            assertEmptyHashBin(hashBin);
            // 插入元素
            List<ByteBuffer> items = HashBinTestUtils.randomByteBufferItems(1);
            boolean success = false;
            while (!success) {
                try {
                    hashBin.insertItems(items);
                    success = true;
                } catch (ArithmeticException ignored) {
                    keys = CommonUtils.generateRandomKeys(
                        CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_ONE_HASH), HashBinTestUtils.SECURE_RANDOM
                    );
                    hashBin = CuckooHashBinFactory.createCuckooHashBin(
                        EnvType.STANDARD, CuckooHashBinType.NO_STASH_ONE_HASH, 1, DEFAULT_BIN_NUM, keys
                    );
                }
            }
            assertInsertedCuckooHashBin(hashBin, items);
            assertHashBinPosition(hashBin, items, keys);
            // 插入虚拟元素
            hashBin.insertPaddingItems(HashBinTestUtils.SECURE_RANDOM);
            assertPaddingHashBin(hashBin, items);
            assertHashBinPosition(hashBin, items, keys);
            hashBin.clear();
            assertEmptyHashBin(hashBin);
            // 再次插入元素
            hashBin.insertItems(items);
            assertInsertedCuckooHashBin(hashBin, items);
            assertHashBinPosition(hashBin, items, keys);
            // 插入空元素
            hashBin.insertPaddingItems(ByteBuffer.wrap(new byte[CommonConstants.BLOCK_BYTE_LENGTH]));
            assertPaddingHashBin(hashBin, items);
            assertHashBinPosition(hashBin, items, keys);
            hashBin.clear();
            // 插入较小数量的元素
            items.remove(0);
            hashBin.insertItems(items);
            assertInsertedCuckooHashBin(hashBin, items);
            assertHashBinPosition(hashBin, items, keys);
            hashBin.clear();
            // 插入0个元素
            List<ByteBuffer> emptyItems = new LinkedList<>();
            hashBin.insertItems(emptyItems);
            assertInsertedCuckooHashBin(hashBin, emptyItems);
            assertHashBinPosition(hashBin, emptyItems, keys);
            hashBin.clear();
        }
    }

    private void assertEmptyHashBin(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        // 验证状态
        Assert.assertFalse(cuckooHashBin.insertedItems());
        Assert.assertFalse(cuckooHashBin.insertedPaddingItems());
        // 验证数量
        Assert.assertEquals(0, cuckooHashBin.size());
        Assert.assertEquals(0, cuckooHashBin.itemSize());
        Assert.assertEquals(0, cuckooHashBin.itemNumInBins());
        Assert.assertEquals(0, cuckooHashBin.itemNumInStash());
        Assert.assertEquals(0, cuckooHashBin.paddingItemSize());
    }

    private void assertInsertedCuckooHashBin(CuckooHashBin<ByteBuffer> cuckooHashBin, List<ByteBuffer> items) {
        // 验证状态
        Assert.assertTrue(cuckooHashBin.insertedItems());
        Assert.assertFalse(cuckooHashBin.insertedPaddingItems());
        // 验证插入元素数量
        Assert.assertEquals(items.size(), cuckooHashBin.itemSize());
        Assert.assertEquals(0, cuckooHashBin.paddingItemSize());
        Assert.assertEquals(items.size(), cuckooHashBin.size());
        Assert.assertEquals(items.size(), cuckooHashBin.itemNumInBins() + cuckooHashBin.itemNumInStash());
        Assert.assertTrue(cuckooHashBin.itemNumInStash() <= cuckooHashBin.stashSize());
        // 并发验证所有的元素都在布谷鸟哈希桶中
        items.stream().parallel().forEach(item -> Assert.assertTrue(cuckooHashBin.contains(item)));
    }

    private void assertPaddingHashBin(CuckooHashBin<ByteBuffer> cuckooHashBin, List<ByteBuffer> items) {
        // 验证状态
        Assert.assertTrue(cuckooHashBin.insertedItems());
        Assert.assertTrue(cuckooHashBin.insertedPaddingItems());
        // 验证元素数量
        Assert.assertEquals(items.size(), cuckooHashBin.itemSize());
        Assert.assertEquals(cuckooHashBin.binNum() + cuckooHashBin.stashSize(), cuckooHashBin.size());
        Assert.assertEquals(
            cuckooHashBin.binNum() + cuckooHashBin.stashSize() - items.size(), cuckooHashBin.paddingItemSize()
        );
        // 并发验证所有的元素都在布谷鸟哈希桶中
        items.stream().parallel().forEach(item -> Assert.assertTrue(cuckooHashBin.contains(item)));
    }

    private void assertHashBinPosition(CuckooHashBin<ByteBuffer> cuckooHashBin, List<ByteBuffer> items, byte[][] keys) {
        // 外部初始化哈希函数，计算位置，验证外部计算的结果与内部计算结果相同
        Prf[] hashes = Arrays.stream(keys).map(key -> {
                Prf prf = PrfFactory.createInstance(EnvType.STANDARD, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        items.forEach(item -> {
            int[] positions = IntStream.range(0, keys.length)
                .map(index -> hashes[index].getInteger(ObjectUtils.objectToByteArray(item), cuckooHashBin.binNum()))
                .toArray();
            Set<ByteBuffer> positionItems = Arrays.stream(positions)
                .mapToObj(cuckooHashBin::getHashBinEntry)
                .filter(Objects::nonNull)
                .map(HashBinEntry::getItem)
                .collect(Collectors.toSet());
            positionItems.addAll(
                cuckooHashBin.getStash().stream().map(HashBinEntry::getItem).collect(Collectors.toList())
            );
            Assert.assertTrue(positionItems.contains(item));
        });
    }
}

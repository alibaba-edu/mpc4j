package edu.alibaba.mpc4j.common.structure.pgm;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.carrotsearch.hppc.procedures.LongProcedure;
import edu.alibaba.mpc4j.common.structure.pgm.LongPgmIndex.LongPgmIndexBuilder;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Tests {@link LongPgmIndex}.
 *
 * <p> Forked from HPPC commit c9497dfabff240787aa0f5ac7a8f4ad70117ea72.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
public class LongPgmIndexTest {

    @Test
    public void testSanityOneSegmentLevel() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034};
        LongArrayList keyList = new LongArrayList();
        keyList.add(keys, 0, keys.length);
        LongPgmIndexBuilder builder = new LongPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(4)
            .setEpsilonRecursive(2);
        LongPgmIndex pgmIndex = builder.build();
        Assert.assertEquals(keys.length, pgmIndex.size());
        for (long key : keys) {
            Assert.assertTrue(pgmIndex.contains(key));
        }
        Assert.assertFalse(pgmIndex.contains(1));
        Assert.assertFalse(pgmIndex.contains(116));
        Assert.assertFalse(pgmIndex.contains(120));
        Assert.assertFalse(pgmIndex.contains(1190));
        Assert.assertFalse(pgmIndex.contains(1192));
        Assert.assertFalse(pgmIndex.contains(1200));
        Assert.assertFalse(pgmIndex.contains(2000));
        Assert.assertFalse(pgmIndex.contains(4031));
    }

    @Test
    public void testSanityTwoSegmentLevels() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900};
        LongPgmIndexBuilder builder = new LongPgmIndexBuilder()
            .setSortedKeys(keys, keys.length)
            .setEpsilon(1)
            .setEpsilonRecursive(1);
        LongPgmIndex pgmIndex = builder.build();
        Assert.assertEquals(keys.length, pgmIndex.size());
        for (long key : keys) {
            Assert.assertTrue(pgmIndex.contains(key));
        }
    }

    @Test
    public void testRangeIterator() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900};
        LongPgmIndexBuilder builder = new LongPgmIndexBuilder()
            .setSortedKeys(keys, keys.length)
            .setEpsilon(1)
            .setEpsilonRecursive(1);
        LongPgmIndex pgmIndex = builder.build();
        assertIterator(123, 1191, pgmIndex, 123, 1024, 1129, 1191);
        assertIterator(1100, 1300, pgmIndex, 1129, 1191, 1201);
        assertIterator(-1, 100, pgmIndex, 2, 12);
        assertIterator(Integer.MIN_VALUE, 100, pgmIndex, 2, 12);
        assertIterator(Integer.MIN_VALUE, Integer.MAX_VALUE, pgmIndex, 2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900);
        assertIterator(4036, Integer.MAX_VALUE, pgmIndex, 4036, 4037, 4039, 4900);
        assertIterator(4039, 4500, pgmIndex, 4039);
        assertIterator(4040, 4500, pgmIndex);
    }

    private void assertIterator(long minKey, long maxKey, LongPgmIndex pgmIndex, long... expectedKeys) {
        Iterator<LongCursor> iterator = pgmIndex.rangeIterator(minKey, maxKey);
        for (long expectedKey : expectedKeys) {
            Assert.assertTrue(iterator.hasNext());
            Assert.assertEquals(expectedKey, iterator.next().value);
        }
        Assert.assertFalse(iterator.hasNext());
        Assert.assertEquals(expectedKeys.length, pgmIndex.rangeCardinality(minKey, maxKey));
    }

    @Test
    public void testRangeProcedure() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900};
        LongPgmIndexBuilder builder = new LongPgmIndexBuilder()
            .setSortedKeys(keys, keys.length)
            .setEpsilon(1)
            .setEpsilonRecursive(1);
        LongPgmIndex pgmIndex = builder.build();
        assertProcedure(123, 1191, pgmIndex, 123, 1024, 1129, 1191);
        assertProcedure(1100, 1300, pgmIndex, 1129, 1191, 1201);
        assertProcedure(-1, 100, pgmIndex, 2, 12);
        assertProcedure(Integer.MIN_VALUE, 100, pgmIndex, 2, 12);
        assertProcedure(Integer.MIN_VALUE, Integer.MAX_VALUE, pgmIndex, 2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900);
        assertProcedure(4036, Integer.MAX_VALUE, pgmIndex, 4036, 4037, 4039, 4900);
        assertProcedure(4039, 4500, pgmIndex, 4039);
        assertProcedure(4040, 4500, pgmIndex);
    }

    private void assertProcedure(int minKey, int maxKey, LongPgmIndex pgmIndex, long... expectedKeys) {
        LongArrayList processedKeys = new LongArrayList();
        LongProcedure procedure = processedKeys::add;
        pgmIndex.forEachInRange(procedure, minKey, maxKey);
        Assert.assertEquals(LongArrayList.from(expectedKeys), processedKeys);
    }

    @Test
    public void testAgainstHashSet() {
        final Random random = new Random();
        int round = 1;
        for (int i = 0; i < round; i++) {
            long[] additions = new long[1_000_000];
            for (int j = 0; j < additions.length; j++) {
                additions[j] = random.nextLong();
            }
            Arrays.sort(additions);
            // Make sure there is at least one sequence of duplicate keys.
            int originalKeyIndex = random.nextInt(100_000);
            for (int j = 0, numDups = random.nextInt(1_000) + 1; j < numDups; j++) {
                additions[originalKeyIndex + j + 1] = additions[originalKeyIndex];
            }

            LongPgmIndexBuilder builder = new LongPgmIndexBuilder().setSortedKeys(additions, additions.length);
            if (random.nextBoolean()) {
                builder.setEpsilon(random.nextInt(128) + 1);
                builder.setEpsilonRecursive(random.nextInt(16) + 1);
            }
            LongPgmIndex pgmIndex = builder.build();

            TLongSet hashSet = new TLongHashSet();
            for (long addition : additions) {
                hashSet.add(addition);
            }
            Assert.assertEquals(hashSet.size(), pgmIndex.size());
            for (int j = 0; j < additions.length; j++) {
                Assert.assertTrue(String.valueOf(j), pgmIndex.contains(additions[j]));
                Assert.assertEquals(additions[j], additions[pgmIndex.indexOf(additions[j])]);
            }
            random.ints(1_000_000).forEach((key) -> {
                Assert.assertEquals(String.valueOf(key), hashSet.contains(key), pgmIndex.contains(key));
                int index = pgmIndex.indexOf(key);
                if (hashSet.contains(key)) {
                    Assert.assertEquals(key, additions[index]);
                } else {
                    int insertionIndex = -index - 1;
                    Assert.assertTrue(insertionIndex >= 0);
                    Assert.assertTrue(insertionIndex <= additions.length);
                    if (insertionIndex < additions.length) {
                        Assert.assertTrue(String.valueOf(key), additions[insertionIndex] > key);
                    }
                    if (insertionIndex > 0) {
                        Assert.assertTrue(String.valueOf(key), additions[insertionIndex - 1] < key);
                    }
                }
            });
        }
    }

    @Test
    public void testSerialize() {
        final Random random = new Random();
        // serialize empty
        LongPgmIndex expectEmptyPgmIndex = new LongPgmIndexBuilder().build();
        byte[] data = expectEmptyPgmIndex.toByteArray();
        LongPgmIndex actualEmptyPgmIndex = LongPgmIndex.fromByteArray(data);
        Assert.assertEquals(expectEmptyPgmIndex, actualEmptyPgmIndex);
        for (int logSize = 0; logSize <= 20; logSize++) {
            int size = 1 << logSize;
            long[] additions = new long[size];
            for (int j = 0; j < additions.length; j++) {
                additions[j] = random.nextLong();
            }
            Arrays.sort(additions);
            LongPgmIndexBuilder builder = new LongPgmIndexBuilder().setSortedKeys(additions, additions.length);
            if (random.nextBoolean()) {
                builder.setEpsilon(random.nextInt(128) + 1);
                builder.setEpsilonRecursive(random.nextInt(16) + 1);
            }
            LongPgmIndex expectPgmIndex = builder.build();
            data = expectPgmIndex.toByteArray();
            LongPgmIndex actualPgmIndex = LongPgmIndex.fromByteArray(data);
            Assert.assertEquals(expectPgmIndex, actualPgmIndex);
            TLongSet hashSet = new TLongHashSet();
            for (long addition : additions) {
                hashSet.add(addition);
            }
            Assert.assertEquals(hashSet.size(), actualPgmIndex.size());
            for (int j = 0; j < additions.length; j++) {
                Assert.assertTrue(String.valueOf(j), expectPgmIndex.contains(additions[j]));
                Assert.assertEquals(additions[j], additions[expectPgmIndex.indexOf(additions[j])]);
            }
            random.ints(size).forEach((key) -> {
                int index = expectPgmIndex.indexOf(key);
                if (hashSet.contains(key)) {
                    Assert.assertEquals(key, additions[index]);
                } else {
                    int insertionIndex = -index - 1;
                    Assert.assertTrue(insertionIndex >= 0);
                    Assert.assertTrue(insertionIndex <= additions.length);
                    if (insertionIndex < additions.length) {
                        Assert.assertTrue(String.valueOf(key), additions[insertionIndex] > key);
                    }
                    if (insertionIndex > 0) {
                        Assert.assertTrue(String.valueOf(key), additions[insertionIndex - 1] < key);
                    }
                }
            });
        }
    }
}

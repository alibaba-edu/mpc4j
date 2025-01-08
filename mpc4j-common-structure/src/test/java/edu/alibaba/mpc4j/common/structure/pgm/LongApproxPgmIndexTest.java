package edu.alibaba.mpc4j.common.structure.pgm;

import com.carrotsearch.hppc.LongArrayList;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Tests {@link LongApproxPgmIndex}.
 *
 * @author Weiran Liu
 * @date 2024/8/1
 */
public class LongApproxPgmIndexTest {

    @Test
    public void testSanityOneSegmentLevel() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034};
        LongArrayList keyList = new LongArrayList();
        keyList.add(keys, 0, keys.length);
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(4)
            .setEpsilonRecursive(2);
        LongApproxPgmIndex pgmIndex = builder.build();
        Assert.assertEquals(keys.length, pgmIndex.size());
        for (long key : keys) {
            int[] range = pgmIndex.approximateIndexRangeOf(key);
            assertApproximateRange(key, range, keys);
        }
    }

    @Test
    public void testSanityTwoSegmentLevels() {
        long[] keys = new long[]{2, 12, 115, 118, 123, 1024, 1129, 1191, 1201, 4034, 4035, 4036, 4037, 4039, 4900};
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keys)
            .setEpsilon(1)
            .setEpsilonRecursive(1);
        LongApproxPgmIndex pgmIndex = builder.build();
        Assert.assertEquals(keys.length, pgmIndex.size());
        for (long key : keys) {
            int[] range = pgmIndex.approximateIndexRangeOf(key);
            assertApproximateRange(key, range, keys);
        }
    }

    @Test
    public void testLarge() {
        final Random random = new Random();
        int round = 1;
        for (int i = 0; i < round; i++) {
            long[] additions = new long[1_000_000];
            for (int j = 0; j < additions.length; j++) {
                additions[j] = random.nextLong();
            }
            long[] keys = Arrays.stream(additions).distinct().sorted().toArray();
            LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder().setSortedKeys(keys);
            if (random.nextBoolean()) {
                builder.setEpsilon(random.nextInt(128) + 1);
                builder.setEpsilonRecursive(random.nextInt(16) + 1);
            }
            LongApproxPgmIndex pgmIndex = builder.build();
            for (long key : keys) {
                int[] range = pgmIndex.approximateIndexRangeOf(key);
                assertApproximateRange(key, range, keys);
            }
        }
    }

    @Test
    public void testSerialize() {
        final Random random = new Random();
        // serialize empty
        LongApproxPgmIndex expectEmptyPgmIndex = new LongApproxPgmIndexBuilder().build();
        byte[] data = expectEmptyPgmIndex.toByteArray();
        LongApproxPgmIndex actualEmptyPgmIndex = LongApproxPgmIndex.fromByteArray(data);
        Assert.assertEquals(expectEmptyPgmIndex, actualEmptyPgmIndex);
        for (int logSize = 0; logSize <= 20; logSize++) {
            int size = 1 << logSize;
            long[] additions = new long[size];
            for (int j = 0; j < additions.length; j++) {
                additions[j] = random.nextLong();
            }
            long[] keys = Arrays.stream(additions).distinct().sorted().toArray();
            LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder().setSortedKeys(keys);
            if (random.nextBoolean()) {
                builder.setEpsilon(random.nextInt(128) + 1);
                builder.setEpsilonRecursive(random.nextInt(16) + 1);
            }
            LongApproxPgmIndex expectPgmIndex = builder.build();
            data = expectPgmIndex.toByteArray();
            LongApproxPgmIndex actualPgmIndex = LongApproxPgmIndex.fromByteArray(data);
            Assert.assertEquals(expectPgmIndex, actualPgmIndex);
            for (long key : keys) {
                int[] range = actualPgmIndex.approximateIndexRangeOf(key);
                assertApproximateRange(key, range, keys);
            }
        }
    }

    private void assertApproximateRange(long key, int[] range, long[] keys) {
        boolean found = false;
        for (int i = range[1]; i < range[2]; i++) {
            if (keys[i] == key) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }
}

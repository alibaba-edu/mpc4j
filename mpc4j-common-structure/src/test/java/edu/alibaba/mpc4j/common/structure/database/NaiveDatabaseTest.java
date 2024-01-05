package edu.alibaba.mpc4j.common.structure.database;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * naive database test.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public class NaiveDatabaseTest {
    /**
     * default rows
     */
    private static final int DEFAULT_ROWS = 1 << 16;
    /**
     * l array
     */
    private static final int[] L_ARRAY = new int[]{
        1, 5, 7, 9, 15, 16, 17, LongUtils.MAX_L - 1, LongUtils.MAX_L, Long.SIZE, CommonConstants.BLOCK_BIT_LENGTH,
    };
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testZlPartition() {
        for (int l : L_ARRAY) {
            for (int partitionL : L_ARRAY) {
                if (partitionL <= DatabaseFactory.maxBitDatabaseL(DatabaseFactory.DatabaseType.ZL)) {
                    testZlPartition(l, partitionL);
                }
            }
        }
    }

    private void testZlPartition(int l, int partitionL) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, DEFAULT_ROWS, SECURE_RANDOM);
        ZlDatabase[] partitionZlDatabases = database.partitionZl(partitionL);
        // each Zl database has assigned l
        Arrays.stream(partitionZlDatabases).forEach(zlDatabase -> Assert.assertEquals(partitionL, zlDatabase.getL()));
        // combine databases
        NaiveDatabase combinedDatabase = NaiveDatabase.createFromZl(l, partitionZlDatabases);
        Assert.assertEquals(database, combinedDatabase);
    }

    @Test
    public void testZl64Partition() {
        for (int l : L_ARRAY) {
            for (int partitionL : L_ARRAY) {
                if (partitionL <= DatabaseFactory.maxBitDatabaseL(DatabaseFactory.DatabaseType.ZL64)) {
                    testZl64Partition(l, partitionL);
                }
            }
        }
    }

    private void testZl64Partition(int l, int partitionL) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, DEFAULT_ROWS, SECURE_RANDOM);
        Zl64Database[] partitionZl64Databases = database.partitionZl64(partitionL);
        // each Zl64 database has assigned l
        Arrays.stream(partitionZl64Databases).forEach(zl64Database -> Assert.assertEquals(partitionL, zl64Database.getL()));
        // combine databases
        NaiveDatabase combinedDatabase = NaiveDatabase.createFromZl64(l, partitionZl64Databases);
        Assert.assertEquals(database, combinedDatabase);
    }

    @Test
    public void testZl32Partition() {
        for (int l : L_ARRAY) {
            for (int partitionL : L_ARRAY) {
                if (partitionL <= DatabaseFactory.maxBitDatabaseL(DatabaseFactory.DatabaseType.ZL32)) {
                    testZl32Partition(l, partitionL);
                }
            }
        }
    }

    private void testZl32Partition(int l, int partitionL) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, DEFAULT_ROWS, SECURE_RANDOM);
        Zl32Database[] partitionZl32Databases = database.partitionZl32(partitionL);
        // each Zl32 database has assigned l
        Arrays.stream(partitionZl32Databases).forEach(zl32Database -> Assert.assertEquals(partitionL, zl32Database.getL()));
        // combine databases
        NaiveDatabase combinedDatabase = NaiveDatabase.createFromZl32(l, partitionZl32Databases);
        Assert.assertEquals(database, combinedDatabase);
    }
}

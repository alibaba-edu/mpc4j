package edu.alibaba.mpc4j.common.structure.database;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.database.DatabaseFactory.DatabaseType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * bit database tests.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
@RunWith(Parameterized.class)
public class ModBitNumDatabaseTest {
    /**
     * default rows
     */
    private static final int DEFAULT_ROWS = 1 << 16;
    /**
     * min rows
     */
    private static final int MIN_ROWS = 1;
    /**
     * max rows
     */
    private static final int MAX_ROWS = 64;
    /**
     * element bit length array
     */
    private static final int[] L_ARRAY = new int[]{
        1, 5, 7, 9, 15, 16, 17, LongUtils.MAX_L - 1, LongUtils.MAX_L, Long.SIZE, CommonConstants.BLOCK_BIT_LENGTH,
    };
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Zl32 database
        configurations.add(new Object[]{DatabaseType.ZL32.name(), DatabaseType.ZL32});
        // Zl64 database
        configurations.add(new Object[]{DatabaseType.ZL64.name(), DatabaseType.ZL64});
        // Zl database
        configurations.add(new Object[]{DatabaseType.ZL.name(), DatabaseType.ZL});
        // naive database
        configurations.add(new Object[]{DatabaseType.NAIVE.name(), DatabaseType.NAIVE});

        return configurations;
    }

    /**
     * the type
     */
    private final DatabaseType type;
    /**
     * max supported l
     */
    private final int maxL;

    public ModBitNumDatabaseTest(String name, DatabaseType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        maxL = DatabaseFactory.maxBitDatabaseL(type);
    }

    @Test
    public void testIllegalInputs() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                testIllegalInputs(l);
            } else {
                // create a database with l > maxL
                Assert.assertThrows(IllegalArgumentException.class, () -> {
                    int byteL = CommonUtils.getByteLength(l);
                    byte[][] data = IntStream.range(0, DEFAULT_ROWS)
                        .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
                        .toArray(byte[][]::new);
                    DatabaseFactory.create(type, l, data);
                });
                // create a random database with l > maxL
                Assert.assertThrows(IllegalArgumentException.class, () ->
                    DatabaseFactory.createRandom(type, l, DEFAULT_ROWS, SECURE_RANDOM)
                );
                // create an empty database with l > maxL
                Assert.assertThrows(IllegalArgumentException.class, () -> DatabaseFactory.createEmpty(type, l));
            }
        }
        // create a database with l = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            DatabaseFactory.create(type, 0, new byte[DEFAULT_ROWS][0])
        );
        // create a random database with l = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            DatabaseFactory.createRandom(type, 0, DEFAULT_ROWS, SECURE_RANDOM)
        );
        // create an empty database with l = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> DatabaseFactory.createEmpty(type, 0));
    }

    private void testIllegalInputs(int l) {
        // create a database with rows = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            DatabaseFactory.create(type, l, new byte[0][])
        );
        // create a random database with rows = 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            DatabaseFactory.createRandom(type, l, 0, SECURE_RANDOM)
        );
        // create an empty database with l = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> DatabaseFactory.createEmpty(type, 0));
        // create a database with data.l > l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            int largeL = l + 1;
            int largeByteL = CommonUtils.getByteLength(largeL);
            byte[][] data = IntStream.range(0, DEFAULT_ROWS)
                .mapToObj(index -> BytesUtils.randomByteArray(largeByteL, largeL, SECURE_RANDOM))
                .toArray(byte[][]::new);
            DatabaseFactory.create(type, l, data);
        });
        ModBitNumDatabase database = DatabaseFactory.createRandom(type, l, DEFAULT_ROWS, SECURE_RANDOM);
        // split database with split row = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> database.split(0));
        // split database with split row > row
        Assert.assertThrows(IllegalArgumentException.class, () -> database.split(DEFAULT_ROWS + 1));
        // reduce database with reduce row = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> database.reduce(0));
        // reduce database with reduce row > row
        Assert.assertThrows(IllegalArgumentException.class, () -> database.reduce(DEFAULT_ROWS + 1));
        // merge two database with different l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ModBitNumDatabase mergeDatabase = DatabaseFactory.createRandom(type, l + 1, DEFAULT_ROWS, SECURE_RANDOM);
            database.merge(mergeDatabase);
        });
    }

    @Test
    public void testType() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                testType(l);
            }
        }
    }

    private void testType(int l) {
        // database with assigned data
        int byteL = CommonUtils.getByteLength(l);
        byte[][] data = IntStream.range(0, DEFAULT_ROWS)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        ModBitNumDatabase database = DatabaseFactory.create(type, l, data);
        Assert.assertEquals(type, database.getType());
        // random database
        ModBitNumDatabase randomDatabase = DatabaseFactory.createRandom(type, l, DEFAULT_ROWS, SECURE_RANDOM);
        Assert.assertEquals(type, randomDatabase.getType());
        // empty database
        ModBitNumDatabase emptyDatabase = DatabaseFactory.createEmpty(type, l);
        Assert.assertEquals(type, emptyDatabase.getType());
    }

    @Test
    public void testDatabase() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                // empty database
                ModBitNumDatabase emptyDatabase = DatabaseFactory.createEmpty(type, l);
                assertDatabase(l, 0, emptyDatabase);
                // database and random database
                for (int rows = MIN_ROWS; rows < MAX_ROWS; rows++) {
                    testDatabase(l, rows);
                }
            }
        }
    }

    private void testDatabase(int l, int rows) {
        // database with assigned data
        int byteL = CommonUtils.getByteLength(l);
        byte[][] data = IntStream.range(0, rows)
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        ModBitNumDatabase database = DatabaseFactory.create(type, l, data);
        assertDatabase(l, rows, database);
        // random database
        ModBitNumDatabase randomDatabase = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        assertDatabase(l, rows, randomDatabase);
    }

    private void assertDatabase(int l, int rows, ModBitNumDatabase database) {
        // test rows
        Assert.assertEquals(rows, database.rows());
        // test l
        Assert.assertEquals(l, database.getL());
        // test each element
        IntStream.range(0, rows).forEach(rowIndex -> {
            // test bytes data
            byte[] bytesData = database.getBytesData(rowIndex);
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(bytesData, database.getByteL(), database.getL()));
            // test bigIntegerData
            BigInteger bigIntegerData = database.getBigIntegerData(rowIndex);
            Assert.assertTrue(bigIntegerData.signum() >= 0 && bigIntegerData.bitLength() <= database.getL());
            Assert.assertEquals(bigIntegerData, BigIntegerUtils.byteArrayToNonNegBigInteger(bytesData));
        });
    }

    @Test
    public void testReduce() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                for (int rows = MIN_ROWS; rows < MAX_ROWS; rows++) {
                    testReduce(l, rows);
                }
            }
        }
    }

    private void testReduce(int l, int rows) {
        // reduce 1
        ModBitNumDatabase database1 = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        database1.reduce(1);
        Assert.assertEquals(1, database1.rows());
        // reduce all
        ModBitNumDatabase databaseAll = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        databaseAll.reduce(rows);
        Assert.assertEquals(rows, databaseAll.rows());
        if (rows > 1) {
            // reduce rows - 1
            ModBitNumDatabase databaseRows = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
            databaseRows.reduce(rows - 1);
            Assert.assertEquals(rows - 1, databaseRows.rows());
            // reduce half
            ModBitNumDatabase databaseHalf = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
            databaseHalf.reduce(rows / 2);
            Assert.assertEquals(rows / 2, databaseHalf.rows());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                testAllEmptyMerge(l);
            }
        }
    }

    private void testAllEmptyMerge(int l) {
        ModBitNumDatabase database = DatabaseFactory.createEmpty(type, l);
        ModBitNumDatabase mergeDatabase = DatabaseFactory.createEmpty(type, l);
        database.merge(mergeDatabase);
        Assert.assertEquals(0, database.rows());
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                for (int rows = MIN_ROWS; rows < MAX_ROWS; rows++) {
                    testLeftEmptyMerge(l, rows);
                }
            }
        }
    }

    private void testLeftEmptyMerge(int l, int rows) {
        ModBitNumDatabase database = DatabaseFactory.createEmpty(type, l);
        ModBitNumDatabase mergeDatabase = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        database.merge(mergeDatabase);
        Assert.assertEquals(rows, database.rows());
    }

    @Test
    public void testRightEmptyMerge() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                for (int rows = MIN_ROWS; rows < MAX_ROWS; rows++) {
                    testRightEmptyMerge(l, rows);
                }
            }
        }
    }

    private void testRightEmptyMerge(int l, int rows) {
        ModBitNumDatabase database = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        ModBitNumDatabase mergeDatabase = DatabaseFactory.createEmpty(type, l);
        database.merge(mergeDatabase);
        Assert.assertEquals(rows, database.rows());
    }

    @Test
    public void testMerge() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                for (int rows1 = MIN_ROWS; rows1 < MAX_ROWS; rows1++) {
                    for (int rows2 = MIN_ROWS; rows2 < MAX_ROWS; rows2++) {
                        testMerge(l, rows1, rows2);
                    }
                }
            }
        }
    }

    private void testMerge(int l, int rows1, int rows2) {
        ModBitNumDatabase database = DatabaseFactory.createRandom(type, l, rows1, SECURE_RANDOM);
        ModBitNumDatabase mergeDatabase = DatabaseFactory.createRandom(type, l, rows2, SECURE_RANDOM);
        database.merge(mergeDatabase);
        Assert.assertEquals(rows1 + rows2, database.rows());
    }

    @Test
    public void testSplit() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                for (int rows = MIN_ROWS; rows < MAX_ROWS; rows++) {
                    testSplit(l, rows);
                }
            }
        }
    }

    private void testSplit(int l, int rows) {
        // split 1
        ModBitNumDatabase database1 = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        ModBitNumDatabase splitDatabase1 = database1.split(1);
        Assert.assertEquals(rows - 1, database1.rows());
        Assert.assertEquals(1, splitDatabase1.rows());
        // split all
        ModBitNumDatabase databaseAll = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
        ModBitNumDatabase splitDatabaseAll = databaseAll.split(rows);
        Assert.assertEquals(0, databaseAll.rows());
        Assert.assertEquals(rows, splitDatabaseAll.rows());
        if (rows > 1) {
            // split rows - 1
            ModBitNumDatabase databaseRows = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
            ModBitNumDatabase splitDatabaseRows = databaseRows.split(rows - 1);
            Assert.assertEquals(1, databaseRows.rows());
            Assert.assertEquals(rows - 1, splitDatabaseRows.rows());
            // split half
            ModBitNumDatabase databaseHalf = DatabaseFactory.createRandom(type, l, rows, SECURE_RANDOM);
            ModBitNumDatabase splitDatabaseHalf = databaseHalf.split(rows / 2);
            Assert.assertEquals(rows - rows / 2, databaseHalf.rows());
            Assert.assertEquals(rows / 2, splitDatabaseHalf.rows());
        }
    }

    @Test
    public void testBitPartition() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                testBitPartition(l);
            }
        }
    }

    private void testBitPartition(int l) {
        ModBitNumDatabase database = DatabaseFactory.createRandom(type, l, DEFAULT_ROWS, SECURE_RANDOM);
        BitVector[] bitVectors = database.bitPartition(EnvType.STANDARD, true);
        ModBitNumDatabase combinedDatabase = DatabaseFactory.create(type, EnvType.STANDARD, true, bitVectors);
        Assert.assertEquals(database, combinedDatabase);
    }
}

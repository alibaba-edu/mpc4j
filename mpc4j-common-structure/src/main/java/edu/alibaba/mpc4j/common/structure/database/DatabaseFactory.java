package edu.alibaba.mpc4j.common.structure.database;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * database factory.
 *
 * @author Weiran Liu
 * @date 2023/4/5
 */
public class DatabaseFactory {
    /**
     * private constructor.
     */
    private DatabaseFactory() {
        // empty
    }

    /**
     * database type
     */
    public enum DatabaseType {
        /**
         * naive database
         */
        NAIVE,
        /**
         * Zl32 database
         */
        ZL32,
        /**
         * Zl64 database
         */
        ZL64,
        /**
         * Zl database
         */
        ZL,
    }

    /**
     * Gets supported max l.
     *
     * @param type the type.
     * @return supported max l.
     */
    public static int maxBitDatabaseL(DatabaseType type) {
        switch (type) {
            case ZL32:
                return IntUtils.MAX_L;
            case ZL64:
                return LongUtils.MAX_L;
            case ZL:
            case NAIVE:
                return Integer.MAX_VALUE;
            default:
                throw new IllegalArgumentException("Invalid " + type.getClass().getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a database.
     *
     * @param type the type.
     * @param l    number of columns.
     * @param data data.
     * @return a database.
     */
    public static ModBitNumDatabase create(DatabaseType type, int l, byte[][] data) {
        switch (type) {
            case ZL32:
                return Zl32Database.create(l, data);
            case ZL64:
                return Zl64Database.create(l, data);
            case ZL:
                return ZlDatabase.create(l, data);
            case NAIVE:
                return NaiveDatabase.create(l, data);
            default:
                throw new IllegalArgumentException("Invalid " + type.getClass().getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a random database.
     *
     * @param type         the type.
     * @param l            number of columns.
     * @param rows         number of rows.
     * @param secureRandom the random state.
     * @return a database.
     */
    public static ModBitNumDatabase createRandom(DatabaseType type, int l, int rows, SecureRandom secureRandom) {
        switch (type) {
            case ZL32:
                return Zl32Database.createRandom(l, rows, secureRandom);
            case ZL64:
                return Zl64Database.createRandom(l, rows, secureRandom);
            case ZL:
                return ZlDatabase.createRandom(l, rows, secureRandom);
            case NAIVE:
                return NaiveDatabase.createRandom(l, rows, secureRandom);
            default:
                throw new IllegalArgumentException("Invalid " + type.getClass().getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an empty database.
     *
     * @param type the type.
     * @param l number of rows.
     * @return a database.
     */
    public static ModBitNumDatabase createEmpty(DatabaseType type, int l) {
        switch (type) {
            case ZL32:
                return Zl32Database.createEmpty(l);
            case ZL64:
                return Zl64Database.createEmpty(l);
            case ZL:
                return ZlDatabase.createEmpty(l);
            case NAIVE:
                return NaiveDatabase.createEmpty(l);
            default:
                throw new IllegalArgumentException("Invalid " + type.getClass().getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a database by combining bit vectors.
     *
     * @param type       the type.
     * @param envType    the environment.
     * @param parallel   parallel combination.
     * @param bitVectors the combining bit vectors.
     * @return a database.
     */
    public static ModBitNumDatabase create(DatabaseType type, EnvType envType, boolean parallel, BitVector... bitVectors) {
        switch (type) {
            case ZL32:
                return Zl32Database.create(envType, parallel, bitVectors);
            case ZL64:
                return Zl64Database.create(envType, parallel, bitVectors);
            case ZL:
                return ZlDatabase.create(envType, parallel, bitVectors);
            case NAIVE:
                return NaiveDatabase.create(envType, parallel, bitVectors);
            default:
                throw new IllegalArgumentException("Invalid " + type.getClass().getSimpleName() + ": " + type.name());
        }
    }
}

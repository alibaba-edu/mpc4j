package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * utilities for 128-bit block operations.
 *
 * @author Weiran Liu
 * @date 2024/10/18
 */
public class BlockUtils {
    /**
     * byte length
     */
    public static final int BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long length
     */
    public static final int LONG_LENGTH = CommonConstants.BLOCK_LONG_LENGTH;
    /**
     * unsafe API.
     * See <a href="https://howtodoinjava.com/java-examples/usage-of-class-sun-misc-unsafe/">Usage of class sun.misc.Unsafe</a>
     * for a good explanations with examples. We also note that <code>Unsafe</code> locates in different places in JDK 8
     * and JDK 17.
     */
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * private constructor.
     */
    private BlockUtils() {
        // empty
    }

    /**
     * Clones a block.
     *
     * @param x x.
     * @return cloned x.
     */
    public static byte[] clone(byte[] x) {
        assert x.length == BYTE_LENGTH;
        byte[] y = new byte[BYTE_LENGTH];
        System.arraycopy(x, 0, y, 0, BYTE_LENGTH);
        return y;
    }

    /**
     * Computes x ⊙ y and places the result into x.
     *
     * @param x x.
     * @param y y.
     */
    public static void xori(long[] x, byte[] y) {
        assert x.length == LONG_LENGTH;
        assert y.length == BYTE_LENGTH;
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            x[j] ^= UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
        }
    }

    /**
     * Converts block (x) represented by <code>long[]</code> to block (y) represented by <code>byte[]</code>.
     *
     * @param x block represented by <code>long[]</code>.
     * @param y block represented by <code>byte[]</code>.
     */
    public static void toByteArray(long[] x, byte[] y) {
        assert x.length == LONG_LENGTH;
        assert y.length == BYTE_LENGTH;
        UNSAFE.copyMemory(x, Unsafe.ARRAY_LONG_BASE_OFFSET, y, Unsafe.ARRAY_BYTE_BASE_OFFSET, BYTE_LENGTH);
    }

    /**
     * In-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    public static byte[] xor(byte[] x, byte[] y) {
        assert x.length == BYTE_LENGTH;
        assert y.length == BYTE_LENGTH;
        byte[] z = new byte[BYTE_LENGTH];
        // trivial implementation is the fastest one
        for (int j = 0; j < BYTE_LENGTH; j++) {
            z[j] = (byte) (x[j] ^ y[j]);
        }
        return z;
    }

    /**
     * In-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    public static void xori(byte[] x, byte[] y) {
        assert x.length == BYTE_LENGTH;
        assert y.length == BYTE_LENGTH;
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            long xLong = UNSAFE.getLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            long yLong = UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            xLong ^= yLong;
            UNSAFE.getAndSetLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j, xLong);
        }
    }

    /**
     * AND operation.
     *
     * @param x x.
     * @param y y.
     * @return x AND y.
     */
    public static byte[] and(byte[] x, byte[] y) {
        assert x.length == BYTE_LENGTH;
        assert y.length == BYTE_LENGTH;
        byte[] z = new byte[BYTE_LENGTH];
        // trivial implementation is the fastest one
        for (int j = 0; j < BYTE_LENGTH; j++) {
            z[j] = (byte) (x[j] & y[j]);
        }
        return z;
    }

    /**
     * In-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    public static void andi(byte[] x, byte[] y) {
        assert x.length == BYTE_LENGTH;
        assert y.length == BYTE_LENGTH;
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            long xLong = UNSAFE.getLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            long yLong = UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            xLong &= yLong;
            UNSAFE.getAndSetLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j, xLong);
        }
    }
}

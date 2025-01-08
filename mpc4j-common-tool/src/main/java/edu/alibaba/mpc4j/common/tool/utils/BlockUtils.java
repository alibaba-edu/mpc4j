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
    private static final int BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long length
     */
    private static final int LONG_LENGTH = CommonConstants.BLOCK_LONG_LENGTH;
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
}

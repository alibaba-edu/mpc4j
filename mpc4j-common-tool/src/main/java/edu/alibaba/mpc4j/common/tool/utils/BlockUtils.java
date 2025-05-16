package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

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
     * Creates 0 block represented as <code>byte[]</code>.
     *
     * @return 0 block represented as <code>byte[]</code>.
     */
    public static byte[] zeroBlock() {
        return new byte[BYTE_LENGTH];
    }

    /**
     * Creates 0 blocks represented as <code>byte[]</code>.
     *
     * @param num number of 0 blocks.
     * @return 0 blocks represented as <code>byte[]</code>.
     */
    public static byte[][] zeroBlocks(int num) {
        return new byte[num][BYTE_LENGTH];
    }

    /**
     * Creates all-one block represented as <code>byte[]</code>.
     *
     * @return all-one block represented as <code>byte[]</code>.
     */
    public static byte[] allOneBlock() {
        return new byte[]{
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        };
    }

    /**
     * Creates an empty block represented as <code>long[]</code>.
     *
     * @return an empty block represented as <code>long[]</code>.
     */
    public static long[] zeroLongBlock() {
        return new long[LONG_LENGTH];
    }

    /**
     * Creates a random block.
     *
     * @param secureRandom random state.
     * @return a random block.
     */
    public static byte[] randomBlock(SecureRandom secureRandom) {
        byte[] x = new byte[BYTE_LENGTH];
        secureRandom.nextBytes(x);
        return x;
    }

    /**
     * Creates random blocks.
     *
     * @param num          num.
     * @param secureRandom random state.
     * @return random blocks.
     */
    public static byte[][] randomBlocks(int num, SecureRandom secureRandom) {
        return IntStream.range(0, num)
            .mapToObj(index -> randomBlock(secureRandom))
            .toArray(byte[][]::new);
    }

    /**
     * Checks if x is a valid block.
     *
     * @param x x.
     * @return if x is a valid block.
     */
    public static boolean valid(byte[] x) {
        return x.length == BYTE_LENGTH;
    }

    /**
     * Checks if x is a valid block array.
     *
     * @param x x.
     * @return if x is a valid block array.
     */
    public static boolean valid(byte[][] x) {
        for (byte[] bytes : x) {
            if (bytes.length != BYTE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if x is a valid block.
     *
     * @param x x.
     * @return if x is a valid block.
     */
    public static boolean valid(long[] x) {
        return x.length == LONG_LENGTH;
    }

    /**
     * Checks if x and y are equal.
     *
     * @param x x.
     * @param y y.
     * @return if x and y are equal.
     */
    public static boolean equals(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        return Arrays.equals(x, y);
    }

    /**
     * Clones a block.
     *
     * @param x x.
     * @return cloned x.
     */
    public static byte[] clone(byte[] x) {
        assert valid(x);
        byte[] y = new byte[BYTE_LENGTH];
        System.arraycopy(x, 0, y, 0, BYTE_LENGTH);
        return y;
    }

    /**
     * Clones a block array.
     *
     * @param x x.
     * @return cloned x.
     */
    public static byte[][] clone(byte[][] x) {
        assert valid(x);
        byte[][] y = new byte[x.length][BYTE_LENGTH];
        for (int i = 0; i < x.length; i++) {
            System.arraycopy(x[i], 0, y[i], 0, BYTE_LENGTH);
        }
        return y;
    }

    /**
     * Converts block (x) represented by <code>byte[]</code> to block (y) represented by <code>long[]</code>.
     *
     * @param x block represented by <code>byte[]</code>.
     * @return block represented by <code>long[]</code>.
     */
    static long[] toLongArray(byte[] x) {
        assert valid(x);
        long[] y = new long[LONG_LENGTH];
        UNSAFE.copyMemory(x, Unsafe.ARRAY_BYTE_BASE_OFFSET, y, Unsafe.ARRAY_LONG_BASE_OFFSET, BYTE_LENGTH);
        return y;
    }

    /**
     * Converts block (x) represented by <code>long[]</code> to block (y) represented by <code>byte[]</code>.
     *
     * @param x block represented by <code>long[]</code>.
     */
    static byte[] toByteArray(long[] x) {
        assert valid(x);
        byte[] y = new byte[BYTE_LENGTH];
        UNSAFE.copyMemory(x, Unsafe.ARRAY_LONG_BASE_OFFSET, y, Unsafe.ARRAY_BYTE_BASE_OFFSET, BYTE_LENGTH);
        return y;
    }

    /**
     * Computes x ⊙ y and places the result into x.
     *
     * @param x x.
     * @param y y.
     */
    static void xori(long[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            x[j] ^= UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
        }
    }

    /**
     * In-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    public static byte[] xor(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        byte[] z = new byte[BYTE_LENGTH];
        // trivial implementation is the fastest one
        for (int j = 0; j < BYTE_LENGTH; j++) {
            z[j] = (byte) (x[j] ^ y[j]);
        }
        return z;
    }

    /**
     * In-place XOR operation. Experiments show that different platforms have different performances. However, on Linux,
     * using naive way invokes auto-vectorization optimization. So, we choose naive way.
     *
     * @param x x.
     * @param y y.
     */
    public static void xori(byte[] x, byte[] y) {
        naiveXori(x, y);
    }

    /**
     * Naive way of in-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    static void naiveXori(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        for (int j = 0; j < BYTE_LENGTH; j++) {
            x[j] ^= y[j];
        }
    }

    /**
     * Unsafe way of in-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    public static void unsafeXori(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
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
        assert valid(x);
        assert valid(y);
        byte[] z = new byte[BYTE_LENGTH];
        // trivial implementation is the fastest one
        for (int j = 0; j < BYTE_LENGTH; j++) {
            z[j] = (byte) (x[j] & y[j]);
        }
        return z;
    }

    /**
     * In-place AND operation. Experiments show that different platforms have different performances. However, on Linux,
     * using naive way invokes auto-vectorization optimization. So, we choose naive way.
     *
     * @param x x.
     * @param y y.
     */
    public static void andi(byte[] x, byte[] y) {
        naiveAndi(x, y);
    }

    /**
     * Naive way of in-place AND operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    static void naiveAndi(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        for (int j = 0; j < BYTE_LENGTH; j++) {
            x[j] &= y[j];
        }
    }

    /**
     * In-place XOR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    static void unsafeAndi(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            long xLong = UNSAFE.getLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            long yLong = UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            xLong &= yLong;
            UNSAFE.getAndSetLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j, xLong);
        }
    }

    /**
     * In-place OR operation. Experiments show that different platforms have different performances. However, on Linux,
     * using naive way invokes auto-vectorization optimization. So, we choose naive way.
     *
     * @param x x.
     * @param y y.
     */
    public static void ori(byte[] x, byte[] y) {
        naiveOri(x, y);
    }

    /**
     * Naive way of in-place OR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    static void naiveOri(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        for (int j = 0; j < BYTE_LENGTH; j++) {
            x[j] |= y[j];
        }
    }

    /**
     * In-place OR operation. The result is placed in x.
     *
     * @param x x.
     * @param y y.
     */
    static void unsafeOri(byte[] x, byte[] y) {
        assert valid(x);
        assert valid(y);
        // use UNSAFE to do operations
        for (int j = 0; j < LONG_LENGTH; j++) {
            long xLong = UNSAFE.getLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            long yLong = UNSAFE.getLong(y, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            xLong |= yLong;
            UNSAFE.getAndSetLong(x, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j, xLong);
        }
    }

    /**
     * In-place shift left.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     */
    public static void shiftLefti(byte[] byteArray, final int x) {
        assert valid(byteArray);
        // here we do not find more efficient ways
        BytesUtils.shiftLefti(byteArray, x);
    }

    /**
     * In-place shift right.
     *
     * @param byteArray byte array.
     * @param x         number of shift bits.
     */
    public static void shiftRighti(byte[] byteArray, final int x) {
        assert valid(byteArray);
        // here we do not find more efficient ways
        BytesUtils.shiftRighti(byteArray, x);
    }
}

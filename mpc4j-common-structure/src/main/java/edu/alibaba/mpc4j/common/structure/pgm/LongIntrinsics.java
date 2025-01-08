package edu.alibaba.mpc4j.common.structure.pgm;

/**
 * Intrinsic methods that are fully functional for long and are replaced with low-level corresponding equivalents for
 * the generated primitive types.
 *
 * <p> Inspired from <code>Intrinsics</code> appearing in HPPC commit c9497dfabff240787aa0f5ac7a8f4ad70117ea72.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
class LongIntrinsics {
    /**
     * private constructor.
     */
    private LongIntrinsics() {
        // empty
    }

    /**
     * Creates a long value that represents empty.
     *
     * @return a long value that represents empty.
     */
    static long empty() {
        return -1L;
    }

    /**
     * Compares two <code>long</code> values numerically.
     *
     * @param x the first <code>long</code> to compare.
     * @param y the second <code>long</code> to compare.
     * @return the value <code>0</code> if <code>x == y</code>; a value less than <code>0</code> if <code>x < y</code>;
     * and a value greater than <code>0</code> if <code>x > y</code>.
     */
    static int compare(long x, long y) {
        return Long.compare(x, y);
    }

    /**
     * Converts the <code>long</code> value to a numeric value that will be used to do computations.
     *
     * @param e the <code>long</code> value to convert.
     * @return result.
     */
    static double numeric(long e) {
        return (double) e;
    }

    /**
     * Multiply two unsigned 64-bit values.
     *
     * <p>The implementation comes from <code>Hash.java</code> in
     * <a href="https://github.com/FastFilter/fastfilter_java">fastfilter_java</a>, see
     * <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8188044">
     *     JDK-8188044: We need Math.unsignedMultiplyHigh</a> for details.
     *
     * @param a the first value.
     * @param b the second value.
     * @return the result.
     */
    static long multiplyHighUnsigned(long a, long b) {
        return Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a);
    }
}

package edu.alibaba.mpc4j.common.structure.fusefilter;

/**
 * byte fuse utilities.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class ByteFuseUtils {
    /**
     * private constructor.
     */
    private ByteFuseUtils() {
        // empty
    }

    /**
     * Calculates segment length for arity = 3, must be a power-of-2.
     *
     * @param size number of elements.
     * @return segment length.
     */
    static int calculateArity3SegmentLength(int size) {
        return 1 << (int) Math.floor(Math.log(size) / Math.log(3.33) + 2.11);
    }

    /**
     * Calculates size factor c for arity = 3.
     *
     * @param size number of elements.
     * @return size factor c.
     */
    static double calculateArity3SizeFactor(int size) {
        return Math.max(1.125, 0.875 + 0.25 * Math.log(1000000) / Math.log(size));
    }

    /**
     * Shrink the hash to a value [0, n).
     * Kind of like modulo, but using multiplication and shift, which are faster to compute.
     *
     * @param hash hash value.
     * @param n    the maximum of the result.
     * @return the reduced value.
     */
    public static int reduce(int hash, int n) {
        // see http://lemire.me/blog/2016/06/27/a-fast-alternative-to-the-modulo-reduction/
        return (int) (((hash & 0xffffffffL) * (n & 0xffffffffL)) >>> 32);
    }

    /**
     * In-place addition.
     *
     * @param p          p.
     * @param q          q.
     * @param byteLength byte length of p and q.
     */
    static void addi(byte[] p, byte[] q, int byteLength) {
        assert p.length == byteLength && q.length == byteLength;
        for (int i = 0; i < byteLength; i++) {
            p[i] += q[i];
        }
    }

    /**
     * In-place subtraction.
     *
     * @param p          p.
     * @param q          q.
     * @param byteLength byte length of p and q.
     */
    static void subi(byte[] p, byte[] q, int byteLength) {
        assert p.length == byteLength && q.length == byteLength;
        for (int i = 0; i < byteLength; i++) {
            p[i] -= q[i];
        }
    }
}

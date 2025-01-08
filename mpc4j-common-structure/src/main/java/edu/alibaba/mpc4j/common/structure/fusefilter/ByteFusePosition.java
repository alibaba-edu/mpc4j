package edu.alibaba.mpc4j.common.structure.fusefilter;

/**
 * byte fuse position.
 *
 * @author Weiran Liu
 * @date 2024/7/26
 */
public interface ByteFusePosition<T> extends ByteFuseInstance {
    /**
     * Gets seed that is used to compute the positions for the input x.
     *
     * @return seed.
     */
    byte[] seed();

    /**
     * Gets positions for the input x. The returned positions must be distinct.
     *
     * @param x input x.
     * @return positions.
     */
    int[] positions(T x);
}

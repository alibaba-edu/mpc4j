package edu.alibaba.mpc4j.common.structure.fusefilter;

/**
 * byte fuse filter.
 *
 * @author Weiran Liu
 * @date 2024/7/25
 */
public interface ByteFuseFilter<T> extends ByteFusePosition<T> {
    /**
     * Gets storage.
     *
     * @return storage.
     */
    byte[][] storage();

    /**
     * Decodes value from the input x.
     *
     * @param x x.
     * @return value.
     */
    byte[] decode(T x);
}

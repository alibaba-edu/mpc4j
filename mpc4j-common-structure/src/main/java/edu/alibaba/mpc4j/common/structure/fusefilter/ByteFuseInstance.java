package edu.alibaba.mpc4j.common.structure.fusefilter;

/**
 * byte fuse instance, used to get parameters.
 *
 * @author Weiran Liu
 * @date 2024/9/2
 */
public interface ByteFuseInstance {
    /**
     * Gets arity.
     *
     * @return arity.
     */
    int arity();

    /**
     * Gets value byte length.
     *
     * @return value byte length.
     */
    int valueByteLength();

    /**
     * Gets length of the filter.
     *
     * @return length of the filter.
     */
    int filterLength();
}

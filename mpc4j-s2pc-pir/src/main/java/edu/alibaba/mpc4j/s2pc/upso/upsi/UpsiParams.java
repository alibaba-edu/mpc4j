package edu.alibaba.mpc4j.s2pc.upso.upsi;

/**
 * UPSI params interface.
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
public interface UpsiParams {
    /**
     * return max client element size.
     *
     * @return max client element size.
     */
    int maxClientElementSize();

    /**
     * return expect server size, this is not strict upper bound.
     *
     * @return expect server size.
     */
    int expectServerSize();
}

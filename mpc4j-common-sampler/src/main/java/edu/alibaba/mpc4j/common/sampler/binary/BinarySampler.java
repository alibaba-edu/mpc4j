package edu.alibaba.mpc4j.common.sampler.binary;

import edu.alibaba.mpc4j.common.sampler.Sampler;

/**
 * 二进制采样接口。
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface BinarySampler extends Sampler {
    /**
     * 采样。
     *
     * @return 采样结果。
     */
    boolean sample();
}

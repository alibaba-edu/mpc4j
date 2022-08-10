package edu.alibaba.mpc4j.common.sampler.integral;

import edu.alibaba.mpc4j.common.sampler.Sampler;

/**
 * 整数采样器接口。
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface IntegralSampler extends Sampler {
    /**
     * 采样。
     *
     * @return 采样结果。
     */
    int sample();
}

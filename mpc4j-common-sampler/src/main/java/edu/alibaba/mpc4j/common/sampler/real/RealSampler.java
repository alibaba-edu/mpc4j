package edu.alibaba.mpc4j.common.sampler.real;

import edu.alibaba.mpc4j.common.sampler.Sampler;

/**
 * 浮点数采样接口。
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface RealSampler extends Sampler {
    /**
     * 采样。
     *
     * @return 采样结果。
     */
    double sample();
}

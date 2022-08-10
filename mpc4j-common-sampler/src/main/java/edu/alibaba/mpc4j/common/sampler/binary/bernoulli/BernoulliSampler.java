package edu.alibaba.mpc4j.common.sampler.binary.bernoulli;

import edu.alibaba.mpc4j.common.sampler.binary.BinarySampler;

/**
 * 伯努利分布采样器：以概率p进行伯努利采样。伯努利分布的输入为概率值p ∈ [0, 1]，输出为{0, 1}，其概率分布为：
 * Pr[f(x|p) = 1] = p。
 * Pr[f(x|p) = 0] = 1 - p。
 *
 * @author Weiran Liu
 * @date 2021/12/27
 */
public interface BernoulliSampler extends BinarySampler {

    /**
     * 返回成功概率p。
     *
     * @return 成功概率p。
     */
    double getP();

    @Override
    default double getMean() {
        return getP();
    }

    @Override
    default double getVariance() {
        return getP() * (1.0 - getP());
    }
}

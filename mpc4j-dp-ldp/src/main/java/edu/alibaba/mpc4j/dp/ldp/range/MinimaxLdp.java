package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.common.sampler.binary.bernoulli.SecureBernoulliSampler;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * Minimax范围LDP机制。方案来自于下述论文：
 * <p>
 * Duchi, John C., Michael I. Jordan, and Martin J. Wainwright. Minimax optimal procedures for locally private
 * estimation. Journal of the American Statistical Association 113, no. 521 (2018): 182-201.
 * </p>
 * 方案描述参加下述论文的Algorithm 1: Duchi et al.'s Solution [14] for One-Dimensional Numeric Data.
 * <p>
 * Wang, Ning, Xiaokui Xiao, Yin Yang, Jun Zhao, Siu Cheung Hui, Hyejin Shin, Junbum Shin, and Ge Yu. Collecting
 * and analyzing multidimensional data with local differential privacy. ICDE 2019, pp. 638-649. IEEE, 2019.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
class MinimaxLdp implements RangeLdp {
    /**
     * 配置项
     */
    private MinimaxLdpConfig minimaxLdpConfig;
    /**
     * 输出绝对值
     */
    private double absOutput;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof MinimaxLdpConfig;
        minimaxLdpConfig = (MinimaxLdpConfig) ldpConfig;
        double epsilon = minimaxLdpConfig.getBaseEpsilon();
        // if u = 1, then t_i^* = (e^ε + 1) / (e^ε - 1), else t_i^* = -1 * (e^ε + 1) / (e^ε - 1)
        absOutput = (Math.exp(epsilon) + 1) / (Math.exp(epsilon) - 1);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        minimaxLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return minimaxLdpConfig;
    }

    @Override
    public double getEpsilon() {
        return minimaxLdpConfig.getBaseEpsilon();
    }

    @Override
    public double randomize(double value) {
        assert value >= -1 && value <= 1 : "value must be in range [-1, 1]";
        // Sample a Bernoulli variable u such that Pr[u = 1] = (e^ε - 1) / (2e^ε + 2) * t_i + 1 / 2
        double epsilon = minimaxLdpConfig.getBaseEpsilon();
        double p = (Math.exp(epsilon) - 1) / (2 * Math.exp(epsilon) + 2) * value + 0.5;
        SecureBernoulliSampler bernoulliSampler = new SecureBernoulliSampler(minimaxLdpConfig.getRandom(), p);
        boolean u = bernoulliSampler.sample();
        return u ? absOutput : -1.0 * absOutput;
    }
}

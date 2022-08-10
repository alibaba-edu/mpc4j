package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.common.sampler.real.laplace.ApacheLaplaceSampler;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * Apache拉普拉斯范围LDP机制。由下述论文描述：
 * <p>
 * Wang, Ning, Xiaokui Xiao, Yin Yang, Jun Zhao, Siu Cheung Hui, Hyejin Shin, Junbum Shin, and Ge Yu. Collecting
 * and analyzing multidimensional data with local differential privacy. ICDE 2019, pp. 638-649. IEEE, 2019.
 * </p>
 * <p>
 * A classic mechanism for enforcing differential privacy is the Laplace Mechanism [16], which can be applied to
 * the LDP setting as follows. For simplicity, assume that each user u_i’s data record ti contains a single numeric
 * attribute whose value lies in range [−1, 1]. In the following, we abuse the notation by using t_i to denote this
 * attribute value. Then, we define a randomized function that outputs a perturbed record t_i^* = t_i + Lap(2 / ε),
 * where Lap(λ) denotes a random variable that follows a Laplace distribution of scale λ.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
class ApacheLaplaceLdp implements RangeLdp {
    /**
     * 配置项
     */
    private ApacheLaplaceLdpConfig apacheLaplaceLdpConfig;
    /**
     * Apache拉普拉斯分布采样器
     */
    private ApacheLaplaceSampler apacheLaplaceSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof ApacheLaplaceLdpConfig;
        apacheLaplaceLdpConfig = (ApacheLaplaceLdpConfig) ldpConfig;
        // 计算放缩系数b = 2 / ε
        double b = 2.0 / apacheLaplaceLdpConfig.getBaseEpsilon();
        // 设置Laplace采样器
        apacheLaplaceSampler = new ApacheLaplaceSampler(apacheLaplaceLdpConfig.getRandomGenerator(), 0.0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        apacheLaplaceSampler.reseed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return apacheLaplaceLdpConfig;
    }

    @Override
    public double getEpsilon() {
        return apacheLaplaceLdpConfig.getBaseEpsilon();
    }

    @Override
    public double randomize(double value) {
        assert value >= -1 && value <= 1 : "value must be in range [-1, 1]";
        // t_i^* = t_i + Lap(2 / ε)
        return apacheLaplaceSampler.sample() + value;
    }
}

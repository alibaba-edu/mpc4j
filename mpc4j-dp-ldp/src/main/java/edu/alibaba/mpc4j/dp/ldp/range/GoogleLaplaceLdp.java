package edu.alibaba.mpc4j.dp.ldp.range;

import com.google.privacy.differentialprivacy.LaplaceNoise;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 谷歌拉普拉斯范围LDP机制，采用谷歌实现的高精度拉普拉斯采样实现的数值型LDP机制。由下述论文描述：
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
class GoogleLaplaceLdp implements RangeLdp {
    /**
     * 配置项
     */
    private GoogleLaplaceLdpConfig googleLaplaceLdpConfig;
    /**
     * 谷歌拉普拉斯噪声
     */
    private LaplaceNoise laplaceNoise;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof GoogleLaplaceLdpConfig;
        googleLaplaceLdpConfig = (GoogleLaplaceLdpConfig) ldpConfig;
        // 设置谷歌Laplace采样器
        laplaceNoise = new LaplaceNoise();
        // 尝试采样一次，注意这里Δf = 2
        laplaceNoise.addNoise(0.0, 2.0, googleLaplaceLdpConfig.getBaseEpsilon(), null);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LdpConfig getLdpConfig() {
        return googleLaplaceLdpConfig;
    }

    @Override
    public double getEpsilon() {
        return googleLaplaceLdpConfig.getBaseEpsilon();
    }

    @Override
    public double randomize(double value) {
        assert value >= -1 && value <= 1 : "value must be in range [-1, 1]";
        // t_i^* = t_i + Lap(2 / ε)
        return laplaceNoise.addNoise(value, 2.0, googleLaplaceLdpConfig.getBaseEpsilon(), null);
    }
}

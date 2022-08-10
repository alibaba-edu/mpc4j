package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

import java.util.Random;

/**
 * 阶梯CDP机制。这是一种优化离散机制，解决了Laplace分布[-0.5, 0.5]之间不是最优噪声的问题。阶梯机制最早于下述论文提出：
 * <p>
 * Quan Geng, Peter Kairouz, Sewoong Oh, Pramod Viswanath. The Staircase Mechanism in Differential Privacy. IEEE
 * Journal of Selected Topics in Signal Processing, 2015, 9(7): 1176-1184.
 * </p>
 * 下述论文给出了阶梯机制的最优噪声（Theorem 5）：γ = 1 / (1 + e^{\epsilon / 2})
 * <p>
 * Quan Geng, Pramod Viswanath. The Optimal Noise-Adding Mechanism in Differential Privacy. IEEE Transactions on
 * Information Theory, 2016, 62(2): 925-951.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/21
 */
class StaircaseCdp implements UnboundRealCdp {
    /**
     * 基础Staircase机制参数类
     */
    private StaircaseCdpConfig staircaseCdpConfig;
    /**
     * b = e^{-ε}
     */
    private double b;
    /**
     * p = γ / (γ + (1 - γ)b)
     */
    private double p;
    /**
     * 伪随机数生成器
     */
    private Random random;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof StaircaseCdpConfig;
        staircaseCdpConfig = (StaircaseCdpConfig) cdpConfig;
        b = Math.exp(-1 * staircaseCdpConfig.getBaseEpsilon());
        double gamma = staircaseCdpConfig.getGamma();
        p = gamma / (gamma + (1.0 - gamma) * b);
        random = staircaseCdpConfig.getRandom();
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        random.setSeed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return staircaseCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // Δf * ε
        return staircaseCdpConfig.getSensitivity() * staircaseCdpConfig.getBaseEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public double randomize(double value) {
        // Generate a random variable with Pr[S = 1] = Pr[S = -1] = 1/2
        double su = random.nextDouble();
        int capitalS = su >= 0.5 ? 1 : -1;
        // Generate a geometric random variable G with Pr[G = i] = (1 - b)b^i for integer i \geq 0, where b = e^{-ε}
        double gu = random.nextDouble();
        int capitalG = Double.valueOf(Math.ceil(Math.log(1 - gu) / Math.log(b))).intValue() - 1;
        // Generate a binary random variable U uniformly distributed in [0,1]
        double capitalU = random.nextDouble();
        // Generate a binary random variable B with Pr[B = 0] = γ / (γ + (1 - γ) * b)
        double bu = random.nextDouble();
        double capitalB = bu <= p ? 0 : 1;

        double gamma = staircaseCdpConfig.getGamma();
        double noise = capitalS * ((1 - capitalB) * ((capitalG + gamma * capitalU))
            + capitalB * (capitalG + gamma + (1 - gamma) * capitalU));

        return noise + value;
    }
}

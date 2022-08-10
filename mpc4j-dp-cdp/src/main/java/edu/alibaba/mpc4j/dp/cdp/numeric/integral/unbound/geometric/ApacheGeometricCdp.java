package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * Apache几何CDP机制。最初由下述论文提出：
 * <p>
 * Arpita Ghosh, Tim Roughgarden, Mukund Sundararajan. Universally Utility-Maximizing Privacy Mechanisms. SIAM
 * Journal on Computing, 2012, 41(6): 1673-1693.
 * </p>
 * 下述论文证明当Δf = 1时，几何机制是最有的（Theorem 12）：
 * <p>
 * Quan Geng, Pramod Viswanath. The Optimal Noise-Adding Mechanism in Differential Privacy. IEEE Transactions on
 * Information Theory, 2016, 62(2): 925-951.
 * </p>
 * 当Δf > 1时，最优机制不唯一，我们可以令ε' = ε / Δf来获得一个最优机制。
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/23
 */
class ApacheGeometricCdp implements GeometricCdp {
    /**
     * Apache几何CDP机制配置项
     */
    private ApacheGeometricCdpConfig apacheGeometricCdpConfig;
    /**
     * Apache几何分布采样器
     */
    private ApacheGeometricSampler apacheGeometricSampler;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof ApacheGeometricCdpConfig;
        apacheGeometricCdpConfig = (ApacheGeometricCdpConfig) cdpConfig;
        // b = 1 / ε
        double b = 1.0 / apacheGeometricCdpConfig.getBaseEpsilon();
        apacheGeometricSampler = new ApacheGeometricSampler(apacheGeometricCdpConfig.getRandomGenerator(), 0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        apacheGeometricCdpConfig.getRandomGenerator().setSeed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return apacheGeometricCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // Δf * ε
        return apacheGeometricCdpConfig.getSensitivity() * apacheGeometricCdpConfig.getBaseEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public int randomize(int value) {
        return apacheGeometricSampler.sample() + value;
    }
}

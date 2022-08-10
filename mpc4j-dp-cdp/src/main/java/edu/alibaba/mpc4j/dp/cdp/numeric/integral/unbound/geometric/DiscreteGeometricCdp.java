package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.DiscreteGeometricSampler;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * 离散几何CDP机制。方案来自于下述论文的第4节，Definition 26: Discrete Laplace，Lemma 27: Discrete Laplace Privacy：
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * </p>
 * Definition 26:
 * <p>
 * Let t > 0. The discrete Laplace distribution with scale parameter t is denoted Lap_Z(t). It is a probability
 * distribution supported on the integers and defined by
 * <p>
 * ∀x ∈ Z, Pr_{X ← Lap_Z(t)}[X = x] = (e^{1 / t} - 1) / (e^{1 / t} + 1) * e^{-|x| / t}
 * </p>
 * </p>
 * Lemma 27:
 * <p>
 * Let ∆, ε > 0. Let q : X^n → Z satisfy |q(x) − q(x')| ≤ ∆ for all x, x' ∈ X^n differing on a single entry. Define a
 * randomized algorithm M: X^n → Z by M(x) = q(x) + Y where Y ← Lap_Z(∆ / ε). Then M satisfies (ε, 0)-differential
 * privacy.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
class DiscreteGeometricCdp implements GeometricCdp {
    /**
     * 离散几何CDP机制配置项
     */
    private DiscreteGeometricCdpConfig discreteGeometricCdpConfig;
    /**
     * 离散几何分布采样器
     */
    private DiscreteGeometricSampler discreteGeometricSampler;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof DiscreteGeometricCdpConfig;
        discreteGeometricCdpConfig = (DiscreteGeometricCdpConfig) cdpConfig;
        discreteGeometricSampler = new DiscreteGeometricSampler(discreteGeometricCdpConfig.getRandom(),
            0, discreteGeometricCdpConfig.getT(), discreteGeometricCdpConfig.getS()
        );
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        discreteGeometricCdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return discreteGeometricCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // Δf / ε = t / s，因此ε = Δf * s / t
        double baseEpsilon = (double)discreteGeometricCdpConfig.getS() / discreteGeometricCdpConfig.getT();
        return discreteGeometricCdpConfig.getSensitivity() * baseEpsilon;
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public int randomize(int value) {
        return discreteGeometricSampler.sample() + value;
    }
}

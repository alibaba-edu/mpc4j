package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.common.sampler.real.laplace.ApacheLaplaceSampler;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 全局映射实数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
class GlobalMapRealLdp implements RealLdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private GlobalMapRealLdpConfig globalMapRealLdpConfig;
    /**
     * Apache拉普拉斯分布采样器
     */
    private ApacheLaplaceSampler apacheLaplaceSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof GlobalMapRealLdpConfig;
        globalMapRealLdpConfig = (GlobalMapRealLdpConfig) ldpConfig;
        // 计算放缩系数b = 2 / ε
        double b = 2.0 / globalMapRealLdpConfig.getBaseEpsilon();
        // 设置Laplace采样器
        apacheLaplaceSampler = new ApacheLaplaceSampler(globalMapRealLdpConfig.getRandomGenerator(), 0.0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        globalMapRealLdpConfig.getRandomGenerator().setSeed(seed);
    }

    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], " +
            "(ε = " + globalMapRealLdpConfig.getBaseEpsilon() + ")-" +
            getClass().getSimpleName() ;
    }

    @Override
    public LdpConfig getLdpConfig() {
        return globalMapRealLdpConfig;
    }

    @Override
    public double randomize(double value) {
        double lowerBound = globalMapRealLdpConfig.getLowerBound();
        double upperBound = globalMapRealLdpConfig.getUpperBound();
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 在真实值上应用敏感度等于1的拉普拉斯机制，并验证结果是否在给定的大小界范围内，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            double noiseValue = apacheLaplaceSampler.sample() + value;
            if (noiseValue >= lowerBound && noiseValue <= upperBound) {
                return noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }

    @Override
    public double getPolicyEpsilon(double x1, double x2) {
        double lowerBound = globalMapRealLdpConfig.getLowerBound();
        double upperBound = globalMapRealLdpConfig.getUpperBound();
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        // Global-map provides ε-dLDP privacy guarantee for any pair of values x,x' ∈ D, where |x' − x| = t, and t, ε > 0.
        return Math.abs(x1 - x2) * globalMapRealLdpConfig.getBaseEpsilon();
    }
}

package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.GeometricSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.JdkGeometricSampler;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 全局映射整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
class GlobalMapIntegralLdp implements IntegralLdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private GlobalMapIntegralLdpConfig integralLdpConfig;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 几何分布采样器
     */
    private GeometricSampler geometricSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof GlobalMapIntegralLdpConfig;
        integralLdpConfig = (GlobalMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 计算放缩系数b = 2 / ε
        double b = 2.0 / integralLdpConfig.getBaseEpsilon();
        // 设置几何分布采样器
        geometricSampler = new JdkGeometricSampler(integralLdpConfig.getRandom(), 0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        integralLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], " +
            "(ε = " + integralLdpConfig.getBaseEpsilon() +  ")-" +
            getClass().getSimpleName() ;
    }

    @Override
    public LdpConfig getLdpConfig() {
        return integralLdpConfig;
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 在真实值上应用敏感度等于1的拉普拉斯机制，并验证结果是否在给定的大小界范围内，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            int noiseValue = geometricSampler.sample() + value;
            if (noiseValue >= lowerBound && noiseValue <= upperBound) {
                return noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        // Global-map provides ε-dLDP privacy guarantee for any pair of values x,x' ∈ D, where |x' − x| = t, and t, ε > 0.
        return Math.abs(x1 - x2) * integralLdpConfig.getBaseEpsilon();
    }
}

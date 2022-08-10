package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.CdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpFactory;

/**
 * 朴素有界整数CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
class NaiveBoundIntegralCdp implements BoundIntegralCdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private NaiveBoundIntegralCdpConfig boundIntegralCdpConfig;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 无界离散整数CDP机制
     */
    private UnboundIntegralCdp unboundIntegralCdp;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof NaiveBoundIntegralCdpConfig;
        boundIntegralCdpConfig = (NaiveBoundIntegralCdpConfig)cdpConfig;
        // 初始化上下界
        lowerBound = boundIntegralCdpConfig.getLowerBound();
        upperBound = boundIntegralCdpConfig.getUpperBound();
        // 初始化离散整数型差分隐私机制
        unboundIntegralCdp = UnboundIntegralCdpFactory.createInstance(boundIntegralCdpConfig.getUnboundIntegralCdpConfig());
    }

    @Override
    public double getEpsilon() {
        // 如果无界整数CDP机制的差分隐私参数为ε，则有界整数CDP机制的差分隐私参数为2ε，Δf由底层机制设置
        return 2 * unboundIntegralCdp.getEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        unboundIntegralCdp.reseed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return boundIntegralCdpConfig;
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        if (lowerBound == upperBound) {
            // 如果上下界相等，则不需要随机化，直接返回结果
            return value;
        }
        // 在真实值上应用敏感度等于1的离散整数机制，并验证结果是否在给定的大小界范围内，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            int noiseValue = unboundIntegralCdp.randomize(value);
            if (noiseValue >= lowerBound && noiseValue <= upperBound) {
                return noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }
}

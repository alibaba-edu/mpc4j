package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpFactory;
import org.apache.commons.math3.util.Precision;

/**
 * 朴素有界实数CDP机制。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
class NaiveBoundRealCdp implements BoundRealCdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private NaiveBoundRealCdpConfig boundRealCdpConfig;
    /**
     * 下界
     */
    private double lowerBound;
    /**
     * 上界
     */
    private double upperBound;
    /**
     * 无界实数差分隐私机制
     */
    private UnboundRealCdp unboundRealCdp;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof NaiveBoundRealCdpConfig;
        boundRealCdpConfig = (NaiveBoundRealCdpConfig)cdpConfig;
        lowerBound = boundRealCdpConfig.getLowerBound();
        upperBound = boundRealCdpConfig.getUpperBound();
        unboundRealCdp = UnboundRealCdpFactory.createInstance(boundRealCdpConfig.getUnboundRealCdpConfig());
    }

    @Override
    public double getEpsilon() {
        // 如果无界实数CDP机制的差分隐私参数为ε，则有界实数CDP机制的差分隐私参数为2ε，Δf由底层机制设置
        return 2 * unboundRealCdp.getEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        unboundRealCdp.reseed(seed);
    }

    @Override
    public double getLowerBound() {
        return boundRealCdpConfig.getLowerBound();
    }

    @Override
    public double getUpperBound() {
        return boundRealCdpConfig.getUpperBound();
    }

    @Override
    public CdpConfig getCdpConfig() {
        return boundRealCdpConfig;
    }

    @Override
    public double randomize(double value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        if (Precision.equals(lowerBound, upperBound, DoubleUtils.PRECISION)) {
            return value;
        }
        // 在真实值上应用敏感度等于1的实数机制，并验证结果是否在给定的大小界范围内，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            double noiseValue = unboundRealCdp.randomize(value);
            if (noiseValue >= lowerBound && noiseValue <= upperBound) {
                return noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }
}

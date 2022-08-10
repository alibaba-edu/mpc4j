package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpConfig;

/**
 * 朴素有界实数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class NaiveBoundRealCdpConfig implements BoundRealCdpConfig {
    /**
     * 底层依赖的无界实数CDP机制配置项
     */
    private final UnboundRealCdpConfig unboundRealCdpConfig;
    /**
     * 下边界
     */
    private final double lowerBound;
    /**
     * 上边界
     */
    private final double upperBound;

    private NaiveBoundRealCdpConfig(Builder builder) {
        unboundRealCdpConfig = builder.unboundRealCdpConfig;
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
    }

    public UnboundRealCdpConfig getUnboundRealCdpConfig() {
        return unboundRealCdpConfig;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    @Override
    public double getSensitivity() {
        return unboundRealCdpConfig.getSensitivity();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveBoundRealCdpConfig> {
        /**
         * 下边界
         */
        private final double lowerBound;
        /**
         * 上边界
         */
        private final double upperBound;
        /**
         * 底层依赖的整数型差分隐私机制参数
         */
        private final UnboundRealCdpConfig unboundRealCdpConfig;

        public Builder(UnboundRealCdpConfig unboundRealCdpConfig, double lowerBound, double upperBound) {
            assert lowerBound <= upperBound : "lower bound must be less than or equal to upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.unboundRealCdpConfig = unboundRealCdpConfig;
        }

        @Override
        public NaiveBoundRealCdpConfig build() {
            return new NaiveBoundRealCdpConfig(this);
        }
    }
}

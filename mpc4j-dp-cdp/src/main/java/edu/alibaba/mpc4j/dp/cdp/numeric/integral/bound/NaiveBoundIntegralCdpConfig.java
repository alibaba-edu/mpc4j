package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpConfig;

/**
 * 朴素有界整数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class NaiveBoundIntegralCdpConfig implements BoundIntegralCdpConfig {
    /**
     * 底层依赖的无界整数CDP机制配置项
     */
    private final UnboundIntegralCdpConfig unboundIntegralCdpConfig;
    /**
     * 下边界
     */
    private final int lowerBound;
    /**
     * 上边界
     */
    private final int upperBound;

    private NaiveBoundIntegralCdpConfig(Builder builder) {
        unboundIntegralCdpConfig = builder.unboundIntegralCdpConfig;
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
    }

    public UnboundIntegralCdpConfig getUnboundIntegralCdpConfig() {
        return unboundIntegralCdpConfig;
    }

    @Override
    public int getLowerBound() {
        return lowerBound;
    }

    @Override
    public int getUpperBound() {
        return upperBound;
    }

    @Override
    public int getSensitivity() {
        return unboundIntegralCdpConfig.getSensitivity();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveBoundIntegralCdpConfig> {
        /**
         * 下边界
         */
        private final int lowerBound;
        /**
         * 上边界
         */
        private final int upperBound;
        /**
         * 底层依赖的无界整数CDP机制配置项
         */
        private final UnboundIntegralCdpConfig unboundIntegralCdpConfig;

        public Builder(UnboundIntegralCdpConfig unboundIntegralCdpConfig, int lowerBound, int upperBound) {
            assert lowerBound <= upperBound : "lower bound must be less than or equal to upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.unboundIntegralCdpConfig = unboundIntegralCdpConfig;
        }

        @Override
        public NaiveBoundIntegralCdpConfig build() {
            return new NaiveBoundIntegralCdpConfig(this);
        }
    }
}

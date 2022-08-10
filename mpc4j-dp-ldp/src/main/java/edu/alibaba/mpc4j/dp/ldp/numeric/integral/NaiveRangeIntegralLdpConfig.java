package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpConfig;

/**
 * 朴素范围整数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public class NaiveRangeIntegralLdpConfig implements IntegralLdpConfig {
    /**
     * 底层依赖的范围LDP机制配置项
     */
    private final RangeLdpConfig rangeLdpConfig;
    /**
     * 下边界
     */
    private final int lowerBound;
    /**
     * 上边界
     */
    private final int upperBound;

    private NaiveRangeIntegralLdpConfig(Builder builder) {
        rangeLdpConfig = builder.rangeLdpConfig;
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
    }

    public RangeLdpConfig getRangeLdpConfig() {
        return rangeLdpConfig;
    }

    @Override
    public int getLowerBound() {
        return lowerBound;
    }

    @Override
    public int getUpperBound() {
        return upperBound;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveRangeIntegralLdpConfig> {
        /**
         * 下边界
         */
        private final int lowerBound;
        /**
         * 上边界
         */
        private final int upperBound;
        /**
         * 底层依赖的范围LDP机制配置项
         */
        private final RangeLdpConfig rangeLdpConfig;

        public Builder(RangeLdpConfig rangeLdpConfig, int lowerBound, int upperBound) {
            assert lowerBound < upperBound : "lower bound must be less than upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.rangeLdpConfig = rangeLdpConfig;
        }

        @Override
        public NaiveRangeIntegralLdpConfig build() {
            return new NaiveRangeIntegralLdpConfig(this);
        }
    }
}

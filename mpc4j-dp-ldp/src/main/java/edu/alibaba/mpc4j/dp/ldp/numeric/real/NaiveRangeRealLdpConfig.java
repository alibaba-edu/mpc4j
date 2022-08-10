package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpConfig;

/**
 * 朴素范围实数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class NaiveRangeRealLdpConfig implements RealLdpConfig {
    /**
     * 底层依赖的范围LDP机制配置项
     */
    private final RangeLdpConfig rangeLdpConfig;
    /**
     * 下边界
     */
    private final double lowerBound;
    /**
     * 上边界
     */
    private final double upperBound;

    private NaiveRangeRealLdpConfig(Builder builder) {
        rangeLdpConfig = builder.rangeLdpConfig;
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
    }

    public RangeLdpConfig getRangeLdpConfig() {
        return rangeLdpConfig;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveRangeRealLdpConfig> {
        /**
         * 下边界
         */
        private final double lowerBound;
        /**
         * 上边界
         */
        private final double upperBound;
        /**
         * 底层依赖的范围LDP机制配置项
         */
        private final RangeLdpConfig rangeLdpConfig;

        public Builder(RangeLdpConfig rangeLdpConfig, double lowerBound, double upperBound) {
            assert lowerBound < upperBound : "lower bound must be less than upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.rangeLdpConfig = rangeLdpConfig;
        }

        @Override
        public NaiveRangeRealLdpConfig build() {
            return new NaiveRangeRealLdpConfig(this);
        }
    }
}

package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * 调整映射实数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
public class AdjMapRealLdpConfig implements RealLdpConfig {
    /**
     * 下边界
     */
    private final double lowerBound;
    /**
     * 上边界
     */
    private final double upperBound;
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 分区长度θ
     */
    private final double theta;
    /**
     * 划分比例α
     */
    private final double alpha;
    /**
     * 伪随机数生成器
     */
    private final RandomGenerator randomGenerator;

    private AdjMapRealLdpConfig(Builder builder) {
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
        baseEpsilon = builder.baseEpsilon;
        theta = builder.theta;
        alpha = builder.alpha;
        randomGenerator = builder.randomGenerator;
    }

    @Override
    public double getLowerBound() {
        return lowerBound;
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public double getTheta() {
        return theta;
    }

    public double getAlpha() {
        return alpha;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AdjMapRealLdpConfig> {
        /**
         * 下边界
         */
        private final double lowerBound;
        /**
         * 上边界
         */
        private final double upperBound;
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 分区长度θ
         */
        private final double theta;
        /**
         * 划分比例α
         */
        private double alpha;
        /**
         * 伪随机数生成器
         */
        private RandomGenerator randomGenerator;

        public Builder(double baseEpsilon, double theta, double lowerBound, double upperBound) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            assert theta > 0 : "θ must be greater than 0: " + theta;
            this.theta = theta;
            assert lowerBound < upperBound : "lower bound must be less than upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            // When Equation 1 is satisfied (α = 1), the probability distribution of output values of Adj-map and
            // Global-map is almost entirely fitted.
            alpha = 1.0;
            randomGenerator = new JDKRandomGenerator();
        }

        public Builder setAlpha(double alpha) {
            assert alpha > 0 : "α must be greater than 0: " + alpha;
            this.alpha = alpha;
            return this;
        }

        public Builder setRandomGenerator(RandomGenerator randomGenerator) {
            this.randomGenerator = randomGenerator;
            return this;
        }

        @Override
        public AdjMapRealLdpConfig build() {
            return new AdjMapRealLdpConfig(this);
        }
    }
}

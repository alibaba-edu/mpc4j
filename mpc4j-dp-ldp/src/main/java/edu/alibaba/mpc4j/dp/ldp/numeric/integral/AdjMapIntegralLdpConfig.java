package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 调整映射整数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
public class AdjMapIntegralLdpConfig implements IntegralLdpConfig {
    /**
     * 下边界
     */
    private final int lowerBound;
    /**
     * 上边界
     */
    private final int upperBound;
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 分区长度θ
     */
    private final int theta;
    /**
     * 划分比例α
     */
    private final double alpha;
    /**
     * 伪随机数生成器
     */
    private final Random random;

    private AdjMapIntegralLdpConfig(Builder builder) {
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
        baseEpsilon = builder.baseEpsilon;
        theta = builder.theta;
        alpha = builder.alpha;
        random = builder.random;
    }

    @Override
    public int getLowerBound() {
        return lowerBound;
    }

    @Override
    public int getUpperBound() {
        return upperBound;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public int getTheta() {
        return theta;
    }

    public double getAlpha() {
        return alpha;
    }

    public Random getRandom() {
        return random;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AdjMapIntegralLdpConfig> {
        /**
         * 下边界
         */
        private final int lowerBound;
        /**
         * 上边界
         */
        private final int upperBound;
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 分区长度θ
         */
        private final int theta;
        /**
         * 划分比例α
         */
        private double alpha;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(double baseEpsilon, int theta, int lowerBound, int upperBound) {
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
            random = new SecureRandom();
        }

        public Builder setAlpha(double alpha) {
            assert alpha > 0 : "α must be greater than 0: " + alpha;
            this.alpha = alpha;
            return this;
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public AdjMapIntegralLdpConfig build() {
            return new AdjMapIntegralLdpConfig(this);
        }
    }
}

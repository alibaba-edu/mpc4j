package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 全局映射整数LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
public class GlobalMapIntegralLdpConfig implements IntegralLdpConfig {
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
     * 伪随机数生成器
     */
    private final Random random;

    private GlobalMapIntegralLdpConfig(Builder builder) {
        lowerBound = builder.lowerBound;
        upperBound = builder.upperBound;
        baseEpsilon = builder.baseEpsilon;
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

    public Random getRandom() {
        return random;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<GlobalMapIntegralLdpConfig> {
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
         * 伪随机数生成器
         */
        private Random random;

        public Builder(double baseEpsilon, int lowerBound, int upperBound) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            assert lowerBound < upperBound : "lower bound must be less than upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public GlobalMapIntegralLdpConfig build() {
            return new GlobalMapIntegralLdpConfig(this);
        }
    }
}

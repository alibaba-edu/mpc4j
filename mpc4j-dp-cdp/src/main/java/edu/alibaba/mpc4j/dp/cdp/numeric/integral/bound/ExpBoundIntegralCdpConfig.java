package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 指数有界整数CDP机制配置项。
 *
 * @author Xiaodong Zhang, Weiran Liu
 * @date 2022/4/24
 */
public class ExpBoundIntegralCdpConfig implements BoundIntegralCdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 敏感度Δf
     */
    private final int sensitivity;
    /**
     * 伪随机数生成器
     */
    private final Random random;
    /**
     * 下边界
     */
    private final int lowerBound;
    /**
     * 上边界
     */
    private final int upperBound;

    private ExpBoundIntegralCdpConfig(Builder builder) {
        this.baseEpsilon = builder.baseEpsilon;
        this.sensitivity = builder.sensitivity;
        this.lowerBound = builder.lowerBound;
        this.upperBound = builder.upperBound;
        this.random = builder.random;
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

    @Override
    public int getSensitivity() {
        return sensitivity;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ExpBoundIntegralCdpConfig> {
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
         * 敏感度Δf
         */
        private final int sensitivity;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(double baseEpsilon, int sensitivity, int lowerBound, int upperBound) {
            assert lowerBound <= upperBound : "lower bound must be less than or equal to upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            assert baseEpsilon > 0 : "ε must be greater than 0";
            this.baseEpsilon = baseEpsilon;
            assert sensitivity > 0 : "Δf must be greater than 0";
            this.sensitivity = sensitivity;
            this.random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public ExpBoundIntegralCdpConfig build() {
            return new ExpBoundIntegralCdpConfig(this);
        }
    }
}

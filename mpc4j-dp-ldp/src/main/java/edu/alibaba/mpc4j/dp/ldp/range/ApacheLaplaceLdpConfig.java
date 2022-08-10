package edu.alibaba.mpc4j.dp.ldp.range;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Apache拉普拉斯范围LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public class ApacheLaplaceLdpConfig implements RangeLdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 伪随机数生成器
     */
    private final RandomGenerator randomGenerator;

    private ApacheLaplaceLdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        randomGenerator = builder.randomGenerator;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ApacheLaplaceLdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 伪随机函数
         */
        private RandomGenerator randomGenerator;

        public Builder(double baseEpsilon) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            randomGenerator = new JDKRandomGenerator();
        }

        public Builder setRandomGenerator(RandomGenerator randomGenerator) {
            this.randomGenerator = randomGenerator;
            return this;
        }

        @Override
        public ApacheLaplaceLdpConfig build() {
            return new ApacheLaplaceLdpConfig(this);
        }
    }
}

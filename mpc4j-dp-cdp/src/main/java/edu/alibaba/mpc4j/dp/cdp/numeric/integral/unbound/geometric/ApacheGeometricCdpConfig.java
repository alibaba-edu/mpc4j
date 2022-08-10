package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Apache几何CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class ApacheGeometricCdpConfig implements GeometricCdpConfig {
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
    private final RandomGenerator randomGenerator;

    private ApacheGeometricCdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        sensitivity = builder.sensitivity;
        randomGenerator = builder.randomGenerator;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    @Override
    public int getSensitivity() {
        return sensitivity;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ApacheGeometricCdpConfig> {
        /**
         * 差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 敏感度Δf
         */
        private final int sensitivity;
        /**
         * 伪随机数生成器
         */
        private RandomGenerator randomGenerator;

        public Builder(double baseEpsilon, int sensitivity) {
            assert baseEpsilon > 0 : "ε = must be greater than 0";
            this.baseEpsilon = baseEpsilon;
            assert sensitivity > 0 : "Δf must be greater than 0";
            this.sensitivity = sensitivity;
            randomGenerator = new JDKRandomGenerator();
        }

        public Builder setRandomGenerator(RandomGenerator randomGenerator) {
            this.randomGenerator = randomGenerator;
            return this;
        }

        @Override
        public ApacheGeometricCdpConfig build() {
            return new ApacheGeometricCdpConfig(this);
        }
    }
}

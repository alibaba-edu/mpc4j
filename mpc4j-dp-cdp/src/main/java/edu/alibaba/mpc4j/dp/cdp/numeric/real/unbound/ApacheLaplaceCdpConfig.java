package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Precision;

/**
 * Apache拉普拉斯CDP机制配置项。
 *
 * @author Xiaodong Zhang, Weiran Liu
 * @date 2022/4/20
 */
public class ApacheLaplaceCdpConfig implements UnboundRealCdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 差分隐私参数δ
     */
    private final double delta;
    /**
     * 敏感度Δf
     */
    private final double sensitivity;
    /**
     * 伪随机数生成器
     */
    private final RandomGenerator randomGenerator;

    private ApacheLaplaceCdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        delta = builder.delta;
        sensitivity = builder.sensitivity;
        randomGenerator = builder.randomGenerator;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public double getDelta() {
        return delta;
    }

    @Override
    public double getSensitivity() {
        return sensitivity;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ApacheLaplaceCdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 差分隐私参数δ
         */
        private Double delta;
        /**
         * 敏感度Δf
         */
        private final double sensitivity;
        /**
         * 伪随机函数
         */
        private RandomGenerator randomGenerator;

        public Builder(double baseEpsilon, double sensitivity) {
            assert baseEpsilon >= 0 : "ε must be greater or equal than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            assert sensitivity > 0 : "Δf must be greater than 0: " + sensitivity;
            this.sensitivity = sensitivity;
            randomGenerator = new JDKRandomGenerator();
            delta = 0.0;
        }

        public Builder setRandomGenerator(RandomGenerator randomGenerator) {
            this.randomGenerator = randomGenerator;
            return this;
        }

        public Builder setDelta(double delta) {
            assert delta >= 0 && delta <= 1 : "δ must be in range [0, 1]: " + delta;
            assert !(Precision.equals(baseEpsilon, 0.0, DoubleUtils.PRECISION)
                && Precision.equals(delta, 0.0, DoubleUtils.PRECISION)) : "ε and δ cannot be both 0";
            this.delta = delta;
            return this;
        }

        @Override
        public ApacheLaplaceCdpConfig build() {
            return new ApacheLaplaceCdpConfig(this);
        }
    }
}

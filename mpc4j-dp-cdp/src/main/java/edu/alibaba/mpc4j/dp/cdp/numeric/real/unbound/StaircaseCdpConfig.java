package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 阶梯CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/21
 */
public class StaircaseCdpConfig implements UnboundRealCdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 敏感度Δf
     */
    private final double sensitivity;
    /**
     * 伪随机数生成器
     */
    private final Random random;
    /**
     * γ = 1 / (1 + e^{ε / 2})或人为设置
     */
    private final double gamma;

    private StaircaseCdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        sensitivity = builder.sensitivity;
        random = builder.random;
        gamma = builder.gamma;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    @Override
    public double getSensitivity() {
        return sensitivity;
    }

    public Random getRandom() {
        return random;
    }

    public double getGamma() {
        return gamma;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<StaircaseCdpConfig> {
        /**
         * 差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 敏感度Δf
         */
        private final double sensitivity;
        /**
         * 伪随机数生成器
         */
        private Random random;
        /**
         * γ = 1 / (1 + e^{ε / 2})或人为设置
         */
        private double gamma;

        public Builder(double baseEpsilon, double sensitivity) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            assert sensitivity > 0 : "Δf must be greater than 0: " + sensitivity;
            this.sensitivity = sensitivity;
            this.random = new SecureRandom();
            // 初始化默认gamma值
            this.gamma = 1.0 / (1.0 + Math.exp(baseEpsilon / 2.0));
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        public Builder setGamma(double gamma) {
            assert gamma >= 0 && gamma <= 1 : "γ must be in range [0, 1]";
            this.gamma = gamma;
            return this;
        }

        @Override
        public StaircaseCdpConfig build() {
            return new StaircaseCdpConfig(this);
        }
    }
}

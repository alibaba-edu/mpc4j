package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 离散几何CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class DiscreteGeometricCdpConfig implements GeometricCdpConfig {
    /**
     * t ∈ Z, t ≥ 1
     */
    private final int t;
    /**
     * s ∈ Z, s ≥ 1
     */
    private final int s;
    /**
     * 敏感度Δf
     */
    private final int sensitivity;
    /**
     * 伪随机数生成器
     */
    private final Random random;

    private DiscreteGeometricCdpConfig(Builder builder) {
        t = builder.t;
        s = builder.s;
        sensitivity = builder.sensitivity;
        random = builder.random;
    }

    @Override
    public int getSensitivity() {
        return sensitivity;
    }

    public Random getRandom() {
        return random;
    }

    public int getT() {
        return t;
    }

    public int getS() {
        return s;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DiscreteGeometricCdpConfig> {
        /**
         * t ∈ Z, t ≥ 1
         */
        private final int t;
        /**
         * s ∈ Z, s ≥ 1
         */
        private final int s;
        /**
         * 敏感度Δf
         */
        private final int sensitivity;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(int t, int s, int sensitivity) {
            assert t >= 1 : "t must be greater or equal to 1";
            this.t = t;
            assert s >= 1 : "s must be greater or equal to 1";
            this.s = s;
            assert sensitivity > 0 : "Δf must be greater than 0";
            this.sensitivity = sensitivity;
            random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public DiscreteGeometricCdpConfig build() {
            return new DiscreteGeometricCdpConfig(this);
        }
    }
}

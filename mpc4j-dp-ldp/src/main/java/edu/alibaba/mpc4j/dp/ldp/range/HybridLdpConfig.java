package edu.alibaba.mpc4j.dp.ldp.range;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 混合范围LDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class HybridLdpConfig implements RangeLdpConfig {
    /**
     * 基础差分隐私参数ε
     */
    private final double baseEpsilon;
    /**
     * 伪随机数生成器
     */
    private final Random random;

    private HybridLdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        random = builder.random;
    }

    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    public Random getRandom() {
        return random;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<HybridLdpConfig> {
        /**
         * 基础差分隐私参数ε
         */
        private final double baseEpsilon;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(double baseEpsilon) {
            assert baseEpsilon > 0 : "ε must be greater than 0: " + baseEpsilon;
            this.baseEpsilon = baseEpsilon;
            random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public HybridLdpConfig build() {
            return new HybridLdpConfig(this);
        }
    }
}

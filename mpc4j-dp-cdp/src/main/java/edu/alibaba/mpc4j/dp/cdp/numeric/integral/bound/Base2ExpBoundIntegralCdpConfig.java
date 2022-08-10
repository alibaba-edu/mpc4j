package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Base2指数有界整数CDP机制配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class Base2ExpBoundIntegralCdpConfig implements BoundIntegralCdpConfig {
    /**
     * 差分隐私参数x
     */
    private final int etaX;
    /**
     * Base2指数机制的差分隐私参数y
     */
    private final int etaY;
    /**
     * Base2指数机制的差分隐私参数z
     */
    private final int etaZ;
    /**
     * Base2指数机制限制采样过程中的数据精度，确保机制的安全性
     */
    private final int precision;
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

    private Base2ExpBoundIntegralCdpConfig(Builder builder) {
        this.lowerBound = builder.lowerBound;
        this.upperBound = builder.upperBound;
        this.etaX = builder.etaX;
        this.etaY = builder.etaY;
        this.etaZ = builder.etaZ;
        this.precision = builder.precision;
        this.sensitivity = builder.sensitivity;
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

    public Random getRandom() {
        return random;
    }

    public int getEtaX() {
        return etaX;
    }

    public int getEtaY() {
        return etaY;
    }

    public int getEtaZ() {
        return etaZ;
    }

    public double getEta() {
        return (-1) * etaZ * DoubleUtils.log2(etaX / Math.pow(2, etaY));
    }

    public int getPrecision() {
        return precision;
    }

    @Override
    public int getSensitivity() {
        return sensitivity;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Base2ExpBoundIntegralCdpConfig> {
        /**
         * 下边界
         */
        private final int lowerBound;
        /**
         * 上边界
         */
        private final int upperBound;
        /**
         * 差分隐私参数x
         */
        private final int etaX;
        /**
         * Base2指数机制的差分隐私参数y
         */
        private final int etaY;
        /**
         * Base2指数机制的差分隐私参数z
         */
        private int etaZ;
        /**
         * Base2指数机制限制采样过程中的数据精度，确保机制的安全性
         */
        private int precision;
        /**
         * 敏感度Δf
         */
        private final int sensitivity;
        /**
         * 伪随机数生成器
         */
        private Random random;

        public Builder(int etaX, int etaY, int sensitivity, int lowerBound, int upperBound) {
            assert lowerBound <= upperBound : "lower bound must be less than or equal to upper bound";
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            assert etaX > 0 : "η_x must be greater than 0";
            assert etaY > 0 : "η_y must be greater than 0";
            assert etaX <= Math.pow(2, etaY) : "η_x / 2^(η_y) must be less or equal than 1";
            this.etaX = etaX;
            this.etaY = etaY;
            etaZ = 1;
            assert sensitivity > 0 : "Δf must be greater than 0";
            this.sensitivity = sensitivity;
            random = new SecureRandom();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        public Builder setEtaZ(int etaZ) {
            assert etaZ > 0 : "η_z must be greater than 0";
            this.etaZ = etaZ;
            return this;
        }

        public Builder setPrecision(int precision) {
            assert precision > 0 : "precision must be greater than 0";
            this.precision = precision;
            return this;
        }

        @Override
        public Base2ExpBoundIntegralCdpConfig build() {
            // 一共可能输出uppBound - lowerBound + 1个元素
            int maxOutput = upperBound - lowerBound + 1;
            int bx = Math.max((int)Math.ceil(Math.log(etaX) / Math.log(2)), 1);
            int theoreticalPrecision = 2 * (etaZ * (etaY + bx)) + maxOutput;
            if (this.precision == 0) {
                this.precision = theoreticalPrecision;
            } else {
                assert precision >= theoreticalPrecision : "Cannot achieve precision: " + precision
                    + ", because the maximum theoretical precision is: " + theoreticalPrecision;
            }

            return new Base2ExpBoundIntegralCdpConfig(this);
        }
    }
}

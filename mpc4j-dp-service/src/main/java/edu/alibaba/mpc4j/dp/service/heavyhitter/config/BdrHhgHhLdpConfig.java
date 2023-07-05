package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Random;
import java.util.Set;

/**
 * Budget-Division Randomization HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public class BdrHhgHhLdpConfig extends BaseHhLdpConfig implements HhgHhLdpConfig {
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * HeavyGuardian random state, used only for the server
     */
    private final Random hgRandom;
    /**
     * the privacy allocation parameter α
     */
    private final double alpha;
    /**
     * γ_h, set to negative if we do not manually set
     */
    private final double gammaH;

    protected BdrHhgHhLdpConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
        hgRandom = builder.hgRandom;
        alpha = builder.alpha;
        gammaH = builder.gammaH;
    }

    @Override
    public int getW() {
        return w;
    }

    @Override
    public int getLambdaH() {
        return lambdaH;
    }

    @Override
    public Random getHgRandom() {
        return hgRandom;
    }

    @Override
    public double getAlpha() {
        return alpha;
    }

    @Override
    public double getGammaH() {
        return gammaH;
    }

    public static class Builder extends BaseHhLdpConfig.Builder {
        /**
         * budget num
         */
        private int w;
        /**
         * λ_h, i.e., the cell num in each bucket
         */
        private int lambdaH;
        /**
         * HeavyGuardian random state, used only for the server
         */
        private Random hgRandom;
        /**
         * the privacy allocation parameter α
         */
        private double alpha;
        /**
         * γ_h, set to negative if we do not manually set
         */
        private double gammaH;

        public Builder(Set<String> domainSet, int k, double windowEpsilon) {
            super(HhLdpFactory.HhLdpType.BDR, domainSet, k, windowEpsilon);
            // set default values
            w = 1;
            lambdaH = k;
            alpha = 1.0 / 3;
            gammaH = -1;
            hgRandom = new Random();
        }

        /**
         * Sets the bucket parameters.
         *
         * @param w the bucket num.
         * @param lambdaH λ_h, i.e., the cell num in each bucket.
         */
        public Builder setBucketParams(int w, int lambdaH) {
            MathPreconditions.checkPositive("w (# of buckets)", w);
            MathPreconditions.checkPositive("λ_h (# of heavy part)", lambdaH);
            MathPreconditions.checkGreaterOrEqual("λ_h * w", lambdaH * w, k);
            this.w = w;
            this.lambdaH = lambdaH;
            return this;
        }

        public Builder setHgRandom(Random hgRandom) {
            this.hgRandom = hgRandom;
            return this;
        }

        public Builder setGammaH(double gammaH) {
            MathPreconditions.checkNonNegativeInRangeClosed("γ_h", gammaH, 1);
            this.gammaH = gammaH;
            return this;
        }

        /**
         * Sets the privacy allocation parameter α.
         *
         * @param alpha the privacy allocation parameter α.
         * @return the builder.
         */
        public Builder setAlpha(double alpha) {
            MathPreconditions.checkPositiveInRange("α", alpha, 1);
            this.alpha = alpha;
            return this;
        }

        @Override
        public BdrHhgHhLdpConfig build() {
            return new BdrHhgHhLdpConfig(this);
        }
    }
}

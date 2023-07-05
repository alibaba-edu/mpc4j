package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Random;
import java.util.Set;

/**
 * Cold-Nomination Randomization HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public class CnrHhgHhLdpConfig extends BaseHhLdpConfig implements HhgHhLdpConfig {
    /**
     * default λ_l
     */
    public static final int DEFAULT_LAMBDA_L = 5;
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * λ_l, i.e., the buffer num in each bucket
     */
    private final int lambdaL;
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

    protected CnrHhgHhLdpConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
        lambdaL = builder.lambdaL;
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

    public int getLambdaL() {
        return lambdaL;
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
         * λ_l, i.e., the buffer num in each bucket
         */
        private int lambdaL;
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
            super(HhLdpFactory.HhLdpType.CNR, domainSet, k, windowEpsilon);
            // set default values
            w = 1;
            lambdaH = k;
            lambdaL = DEFAULT_LAMBDA_L;
            alpha = 1.0 / 3;
            gammaH = -1;
            hgRandom = new Random();
        }

        /**
         * Sets the bucket parameters.
         *
         * @param w       the bucket num.
         * @param lambdaH λ_h, i.e., the cell num in each bucket.
         * @return the builder.
         */
        public Builder setBucketParams(int w, int lambdaH) {
            MathPreconditions.checkPositive("w (# of buckets)", w);
            MathPreconditions.checkPositive("λ_h (# of heavy part)", lambdaH);
            MathPreconditions.checkGreaterOrEqual("λ_h * w", lambdaH * w, k);
            this.w = w;
            this.lambdaH = lambdaH;
            return this;
        }

        /**
         * Sets λ_l, i.e., the buffer num in each bucket.
         *
         * @param lambdaL λ_l.
         * @return the builder.
         */
        public Builder setLambdaL(int lambdaL) {
            MathPreconditions.checkPositive("λ_l (# of light part)", lambdaL);
            this.lambdaL = lambdaL;
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
        public CnrHhgHhLdpConfig build() {
            return new CnrHhgHhLdpConfig(this);
        }
    }
}

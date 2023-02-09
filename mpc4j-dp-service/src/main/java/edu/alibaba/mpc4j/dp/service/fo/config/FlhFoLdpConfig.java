package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Fast Local Hash (OLH) Frequency Oracle LDP config. The only difference between FLH config and OLH config is that we
 * limit the maximum number of candidate hash functions to be k.
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class FlhFoLdpConfig extends BasicFoLdpConfig {
    /**
     * we need an int g, therefore, the maximal ε would be ln(MAX_INT) - 1.
     */
    public static final double MAX_EPSILON = Math.log(Integer.MAX_VALUE) - 1;
    /**
     * maximum number of candidate hash functions k'
     */
    private final int k;
    /**
     * candidate hash functions seeds
     */
    private final int[] hashSeeds;

    private FlhFoLdpConfig(Builder builder) {
        super(builder);
        k = builder.k;
        hashSeeds = IntStream.range(0, k)
            .map(hashIndex -> builder.random.nextInt())
            .toArray();
    }

    /**
     * Gets the maximum number of candidate hash functions (k').
     *
     * @return the maximum number of candidate hash functions (k').
     */
    public int getK() {
        return k;
    }

    /**
     * Gets the hash seeds.
     *
     * @return the hash seeds.
     */
    public int[] getHashSeeds() {
        return hashSeeds;
    }

    public static class Builder extends BasicFoLdpConfig.Builder {
        /**
         * maximum number of candidate hash functions (k').
         */
        private int k;
        /**
         * the randomness for generating the hash seeds.
         */
        private Random random;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
            MathPreconditions.checkLessOrEqual("ε", epsilon, MAX_EPSILON);
            // default k', see Section 7.2 of the paper
            k = 10000;
            random = new Random();
        }

        /**
         * Sets the maximum number of hash functions (k').
         *
         * @param k the maximum number of hash functions (k').
         * @return the builder.
         */
        public Builder setK(int k) {
            return setK(k, new Random());
        }

        /**
         * Sets the maximum number of hash functions (k').
         *
         * @param k the maximum number of hash functions (k').
         * @param random the random state used to generate the hash seeds.
         * @return the builder.
         */
        public Builder setK(int k, Random random) {
            MathPreconditions.checkPositive("# of hash functions", k);
            this.k = k;
            this.random = random;
            return this;
        }

        @Override
        public FlhFoLdpConfig build() {
            return new FlhFoLdpConfig(this);
        }
    }
}

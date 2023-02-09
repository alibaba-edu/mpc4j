package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Apple's Count Mean Sketch (CMS) Frequency Oracle LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/31
 */
public class AppleCmsFoLdpConfig extends BasicFoLdpConfig {
    /**
     * number of hash functions k
     */
    private final int k;
    /**
     * the output bound of the hash functions m
     */
    private final int m;
    /**
     * hash seeds
     */
    private final int[] hashSeeds;

    protected AppleCmsFoLdpConfig(Builder builder) {
        super(builder);
        k = builder.k;
        m = builder.m;
        hashSeeds = IntStream.range(0, k)
            .map(cohortIndex -> builder.random.nextInt())
            .toArray();
    }

    /**
     * Gets the number of hash functions (k).
     *
     * @return the number of hash functions (k).
     */
    public int getK() {
        return k;
    }

    /**
     * Gets the output bound of the hash functions (m).
     *
     * @return the output bound of the hash functions (m).
     */
    public int getM() {
        return m;
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
         * number of hash functions k
         */
        private int k;
        /**
         * the output bound of the hash functions m
         */
        private int m;
        /**
         * the randomness for generating the hash seeds
         */
        private Random random;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
            // default k = 65536
            k = 65536;
            // default m = 1024
            m = 1024;
            // default random
            random = new Random();
        }

        /**
         * Sets the number of hash functions (k) and the output bound of the hash functions (m).
         *
         * @param k the number of hash functions.
         * @param m the output bound of the hash functions
         * @return the builder.
         */
        public Builder setHashes(int k, int m) {
            return setHashes(k, m, new Random());
        }

        /**
         * Sets the number of hash functions (k) and the output bound of the hash functions (m).
         *
         * @param k      the number of hash functions.
         * @param m      the output bound of the hash functions
         * @param random the random state used to generate the hash seeds.
         * @return the builder.
         */
        public Builder setHashes(int k, int m, Random random) {
            MathPreconditions.checkPositive("# of hash functions", k);
            this.k = k;
            MathPreconditions.checkGreaterOrEqual("output bound of hash functions", m, 2);
            this.m = m;
            this.random = random;
            return this;
        }

        @Override
        public AppleCmsFoLdpConfig build() {
            return new AppleCmsFoLdpConfig(this);
        }
    }
}

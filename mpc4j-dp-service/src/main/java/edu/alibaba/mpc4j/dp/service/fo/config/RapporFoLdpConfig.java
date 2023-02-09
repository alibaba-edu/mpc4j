package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.rappor.RapporFoLdpUtils;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * RAPPOR Frequency Oracle LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class RapporFoLdpConfig extends BasicFoLdpConfig {
    /**
     * number of cohorts.
     */
    private final int cohortNum;
    /**
     * number of hashes in each cohort.
     */
    private final int hashNum;
    /**
     * hash seeds
     */
    private final int[][] hashSeeds;
    /**
     * the size of the bloom filter
     */
    private final int m;

    private RapporFoLdpConfig(Builder builder) {
        super(builder);
        cohortNum = builder.cohortNum;
        hashNum = builder.hashNum;
        m = RapporFoLdpUtils.getM(d, hashNum);
        hashSeeds = IntStream.range(0, cohortNum)
            // here we do not need to ensure that all hash values for one item are distinct
            .mapToObj(cohortIndex -> IntStream.range(0, hashNum).map(hashIndex -> builder.random.nextInt()).toArray())
            .toArray(int[][]::new);
    }

    /**
     * Gets the number of cohorts.
     *
     * @return the number of cohorts.
     */
    public int getCohortNum() {
        return cohortNum;
    }

    /**
     * Gets the hash seeds.
     *
     * @return the hash seeds.
     */
    public int[][] getHashSeeds() {
        return hashSeeds;
    }

    /**
     * Gets the size of the bloom filter.
     *
     * @return the size of the bloom filter.
     */
    public int getM() {
        return m;
    }

    /**
     * Gets f, the probability used to perturb bloom filters.
     *
     * @return f.
     */
    public double getF() {
        // f = 2 / (e^{ε / 2k} + 1)
        return 2 / (Math.exp(epsilon / 2 / hashNum) + 1);
    }

    public static class Builder extends BasicFoLdpConfig.Builder {
        /**
         * number of cohorts.
         */
        private int cohortNum;
        /**
         * number of hashes in each cohort.
         */
        private int hashNum;
        /**
         * the randomness for generating the hash seeds
         */
        private Random random;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
            // default cohort num is 8, from pure-LDP
            cohortNum = 8;
            // default hash num is 2
            hashNum = 2;
            // default random
            random = new Random();
        }

        /**
         * Sets the number of cohorts and the number of hashes in each cohort.
         *
         * @param cohortNum the number of cohorts.
         * @param hashNum the number of hashes in each cohort.
         * @return the builder.
         */
        public Builder setHashes(int cohortNum, int hashNum) {
            return setHashes(cohortNum, hashNum, new Random());
        }

        /**
         * Sets the number of cohorts and the number of hashes in each cohort.
         *
         * @param cohortNum the number of cohorts.
         * @param hashNum the number of hashes in each cohort.
         * @param random the random state used to generate the hash seeds.
         * @return the builder.
         */
        public Builder setHashes(int cohortNum, int hashNum, Random random) {
            MathPreconditions.checkPositive("# of cohorts", cohortNum);
            this.cohortNum = cohortNum;
            MathPreconditions.checkGreaterOrEqual("# of hashes", hashNum, 2);
            this.hashNum = hashNum;
            this.random = random;
            return this;
        }

        @Override
        public RapporFoLdpConfig build() {
            return new RapporFoLdpConfig(this);
        }
    }
}

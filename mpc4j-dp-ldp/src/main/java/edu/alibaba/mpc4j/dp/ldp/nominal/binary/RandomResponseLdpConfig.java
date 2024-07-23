package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory.BinaryLdpType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * random response LDP config.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public class RandomResponseLdpConfig implements BinaryLdpConfig {
    /**
     * binary label array list
     */
    private static final ArrayList<String> BINARY_LABEL_ARRAY_LIST = Arrays.stream(new String[] {"0", "1"})
        .collect(Collectors
            .toCollection(ArrayList::new));
    /**
     * binary label set
     */
    private static final Set<String> BINARY_LABEL_SET = new HashSet<>(BINARY_LABEL_ARRAY_LIST);
    /**
     * base ε
     */
    private final double baseEpsilon;
    /**
     * random
     */
    private final Random random;

    private RandomResponseLdpConfig(Builder builder) {
        baseEpsilon = builder.baseEpsilon;
        random = builder.random;
    }

    @Override
    public double getBaseEpsilon() {
        return baseEpsilon;
    }

    @Override
    public Random getRandom() {
        return random;
    }

    @Override
    public ArrayList<String> getLabelArrayList() {
        return BINARY_LABEL_ARRAY_LIST;
    }

    @Override
    public Set<String> getLabelSet() {
        return BINARY_LABEL_SET;
    }

    @Override
    public BinaryLdpType getType() {
        return BinaryLdpType.RANDOM_RESPONSE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RandomResponseLdpConfig> {
        /**
         * base ε
         */
        private final double baseEpsilon;
        /**
         * random
         */
        private Random random;

        public Builder(double baseEpsilon) {
            MathPreconditions.checkPositive("ε", baseEpsilon);
            this.baseEpsilon = baseEpsilon;
            random = new Random();
        }

        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public RandomResponseLdpConfig build() {
            return new RandomResponseLdpConfig(this);
        }
    }
}

package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

/**
 * Binary LDP factor.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public class BinaryLdpFactory {
    /**
     * private constructor.
     */
    private BinaryLdpFactory() {
        // empty
    }

    /**
     * Binary LDP type
     */
    public enum BinaryLdpType {
        /**
         * random response
         */
        RANDOM_RESPONSE,
    }

    /**
     * Creates a binary LDP mechanism.
     *
     * @param config config.
     * @return binary LDP machanism.
     */
    public static BinaryLdp createInstance(BinaryLdpConfig config) {
        BinaryLdpType type = config.getType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RANDOM_RESPONSE:
                RandomResponseLdp randomResponseLdp = new RandomResponseLdp();
                randomResponseLdp.setup(config);
                return randomResponseLdp;
            default:
                throw new IllegalArgumentException("Invalid " + BinaryLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param epsilon epsilon.
     * @return a default config.
     */
    public static BinaryLdpConfig createDefaultConfig(double epsilon) {
        return new RandomResponseLdpConfig.Builder(epsilon).build();
    }
}

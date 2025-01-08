package edu.alibaba.mpc4j.crypto.algs.smprp;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * small-domain PRP factory.
 *
 * @author Weiran Liu
 * @date 2024/8/22
 */
public class SmallDomainPrpFactory {
    /**
     * private constructor.
     */
    private SmallDomainPrpFactory() {
        // empty
    }

    /**
     * small-domain PRP type.
     */
    public enum SmallDomainPrpType {
        /**
         * AD_PRP, used in Incremental Offline/Online PIR, following the same name as in the open-source library.
         */
        AD_PRP,
    }

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @param type    type.
     * @return an instance.
     */
    public static SmallDomainPrp createInstance(EnvType envType, SmallDomainPrpType type) {
        switch (type) {
            case AD_PRP -> {
                return new AdSmallDomainPrp(envType);
            }
            case null, default ->
                throw new IllegalArgumentException("Invalid " + SmallDomainPrpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an instance.
     *
     * @param envType environment.
     * @return an instance.
     */
    public static SmallDomainPrp createInstance(EnvType envType) {
        return new AdSmallDomainPrp(envType);
    }
}

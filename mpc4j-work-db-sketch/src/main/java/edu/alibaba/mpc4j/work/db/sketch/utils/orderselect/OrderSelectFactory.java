package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick.QuickOrderSelectConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick.QuickOrderSelectParty;

/**
 * Factory for creating Order Select protocol instances.
 * Provides methods to create order select parties and configurations with different implementations.
 */
public class OrderSelectFactory {
    /**
     * Enumeration of available Order Select protocol types
     */
    public enum OrderSelectType {
        /**
         * QUICK_ORDER_SELECT implementation - uses quick sort based selection
         */
        QUICK_ORDER_SELECT,
    }

    /**
     * Creates an order select party instance based on the provided configuration
     *
     * @param config    the protocol configuration
     * @param abb3Party the underlying ABB3 party for three-party computation
     * @return a new order select party instance
     */
    public static OrderSelectParty createParty(Abb3Party abb3Party, OrderSelectConfig config) {
        return switch (config.getOrderSelectType()) {
            case QUICK_ORDER_SELECT -> new QuickOrderSelectParty(abb3Party, (QuickOrderSelectConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getSortType() in creating OrderSelectParty");
        };
    }

    /**
     * Creates a default order select configuration
     *
     * @param securityModel the security model to use (MALICIOUS or SEMI_HONEST)
     * @return a default order select configuration
     */
    public static OrderSelectConfig createDefaultConfig(SecurityModel securityModel) {
        return new QuickOrderSelectConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}

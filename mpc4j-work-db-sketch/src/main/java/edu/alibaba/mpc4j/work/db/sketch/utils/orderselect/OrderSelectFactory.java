package edu.alibaba.mpc4j.work.db.sketch.utils.orderselect;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick.QuickOrderSelectConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.orderselect.quick.QuickOrderSelectParty;

/**
 * order select factory
 */
public class OrderSelectFactory {
    /**
     * the protocol type
     */
    public enum OrderSelectType {
        /**
         * quick select
         */
        QUICK_ORDER_SELECT,
    }

    /**
     * Creates an order select party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return an order select party.
     */
    public static OrderSelectParty createParty(Abb3Party abb3Party, OrderSelectConfig config) {
        return switch (config.getOrderSelectType()) {
            case QUICK_ORDER_SELECT -> new QuickOrderSelectParty(abb3Party, (QuickOrderSelectConfig) config);
            default -> throw new IllegalArgumentException("Invalid config.getSortType() in creating OrderSelectParty");
        };
    }

    /**
     * Creates an order select party config
     *
     * @param securityModel security model
     */
    public static OrderSelectConfig createDefaultConfig(SecurityModel securityModel) {
        return new QuickOrderSelectConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }
}

package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfSender;

/**
 * Related-Batch OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class RbopprfFactory {
    /**
     * private constructor.
     */
    private RbopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum RbopprfType {
        /**
         * CGS22, hash num = 3
         */
        CGS22,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static RbopprfSender createSender(Rpc senderRpc, Party receiverParty, RbopprfConfig config) {
        RbopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22:
                return new Cgs22RbopprfSender(senderRpc, receiverParty, (Cgs22RbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + RbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static RbopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, RbopprfConfig config) {
        RbopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22:
                return new Cgs22RbopprfReceiver(receiverRpc, senderParty, (Cgs22RbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + RbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static RbopprfConfig createDefaultConfig() {
        return new Cgs22RbopprfConfig.Builder().build();
    }
}

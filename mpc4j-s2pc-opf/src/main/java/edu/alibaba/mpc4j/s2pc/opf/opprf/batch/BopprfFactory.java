package edu.alibaba.mpc4j.s2pc.opf.opprf.batch;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs.OkvsBopprfConfig;

/**
 * Batch OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class BopprfFactory {
    /**
     * private constructor.
     */
    private BopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum BopprfType {
        /**
         * OKVS-based Batch OPPRF
         */
        OKVS,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static BopprfSender createSender(Rpc senderRpc, Party receiverParty, BopprfConfig config) {
        BopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case OKVS:
                return new OkvsBopprfSender(senderRpc, receiverParty, (OkvsBopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BopprfType.class.getSimpleName() + ": " + type.name());
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
    public static BopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, BopprfConfig config) {
        BopprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case OKVS:
                return new OkvsBopprfReceiver(receiverRpc, senderParty, (OkvsBopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static BopprfConfig createDefaultConfig() {
        return new OkvsBopprfConfig.Builder().build();
    }
}

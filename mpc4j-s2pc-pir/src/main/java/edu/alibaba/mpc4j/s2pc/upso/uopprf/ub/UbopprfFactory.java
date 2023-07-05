package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfSender;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfSender;

/**
 * unbalanced batched OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class UbopprfFactory {
    /**
     * private constructor.
     */
    private UbopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UbopprfType {
        /**
         * OKVS
         */
        OKVS,
        /**
         * PIR
         */
        PIR,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static UbopprfSender createSender(Rpc senderRpc, Party receiverParty, UbopprfConfig config) {
        UbopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsUbopprfSender(senderRpc, receiverParty, (OkvsUbopprfConfig) config);
            case PIR:
                return new PirUbopprfSender(senderRpc, receiverParty, (PirUbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UbopprfType.class.getSimpleName() + ": " + type.name());
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
    public static UbopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, UbopprfConfig config) {
        UbopprfType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsUbopprfReceiver(receiverRpc, senderParty, (OkvsUbopprfConfig) config);
            case PIR:
                return new PirUbopprfReceiver(receiverRpc, senderParty, (PirUbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static UbopprfConfig createDefaultConfig() {
        return new OkvsUbopprfConfig.Builder().build();
    }
}

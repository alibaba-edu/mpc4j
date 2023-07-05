package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22.Cgs22UrbopprfSender;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfReceiver;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfSender;

/**
 * unbalanced related-Batch OPRRF factory.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class UrbopprfFactory {
    /**
     * private constructor.
     */
    private UrbopprfFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UrbopprfType {
        /**
         * CGS22, hash num = 3
         */
        CGS22,
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
    public static UrbopprfSender createSender(Rpc senderRpc, Party receiverParty, UrbopprfConfig config) {
        UrbopprfType type = config.getPtoType();
        switch (type) {
            case CGS22:
                return new Cgs22UrbopprfSender(senderRpc, receiverParty, (Cgs22UrbopprfConfig) config);
            case PIR:
                return new PirUrbopprfSender(senderRpc, receiverParty, (PirUrbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UrbopprfType.class.getSimpleName() + ": " + type.name());
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
    public static UrbopprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, UrbopprfConfig config) {
        UrbopprfType type = config.getPtoType();
        switch (type) {
            case CGS22:
                return new Cgs22UrbopprfReceiver(receiverRpc, senderParty, (Cgs22UrbopprfConfig) config);
            case PIR:
                return new PirUrbopprfReceiver(receiverRpc, senderParty, (PirUrbopprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UrbopprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static UrbopprfConfig createDefaultConfig() {
        return new Cgs22UrbopprfConfig.Builder().build();
    }
}

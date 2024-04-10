package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.upso.okvr.kw.KwOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.kw.KwOkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.kw.KwOkvrSender;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.okvs.OkvsOkvrSender;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrConfig;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrReceiver;
import edu.alibaba.mpc4j.s2pc.upso.okvr.pir.PirOkvrSender;

/**
 * OKVR factory.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public class OkvrFactory {
    /**
     * private constructor.
     */
    private OkvrFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum OkvrType {
        /**
         * OKVS
         */
        OKVS,
        /**
         * PIR
         */
        PIR,
        /**
         * Keyword PIR
         */
        KW,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static OkvrSender createSender(Rpc senderRpc, Party receiverParty, OkvrConfig config) {
        OkvrType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsOkvrSender(senderRpc, receiverParty, (OkvsOkvrConfig) config);
            case PIR:
                return new PirOkvrSender(senderRpc, receiverParty, (PirOkvrConfig) config);
            case KW:
                return new KwOkvrSender(senderRpc, receiverParty, (KwOkvrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OkvrType.class.getSimpleName() + ": " + type.name());
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
    public static OkvrReceiver createReceiver(Rpc receiverRpc, Party senderParty, OkvrConfig config) {
        OkvrType type = config.getPtoType();
        switch (type) {
            case OKVS:
                return new OkvsOkvrReceiver(receiverRpc, senderParty, (OkvsOkvrConfig) config);
            case PIR:
                return new PirOkvrReceiver(receiverRpc, senderParty, (PirOkvrConfig) config);
            case KW:
                return new KwOkvrReceiver(receiverRpc, senderParty, (KwOkvrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OkvrType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @return a default config.
     */
    public static OkvrConfig createDefaultConfig() {
        return new OkvsOkvrConfig.Builder().build();
    }
}

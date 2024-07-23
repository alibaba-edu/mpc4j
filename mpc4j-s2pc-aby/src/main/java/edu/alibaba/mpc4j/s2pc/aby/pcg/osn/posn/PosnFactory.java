package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24.Lll24PosnSender;

/**
 * pre-computed OSN factory.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public class PosnFactory implements PtoFactory {
    /**
     * protocol type
     */
    public enum PosnType {
        /**
         * LLL24
         */
        LLL24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static PosnSender createSender(Rpc senderRpc, Party receiverParty, PosnConfig config) {
        PosnType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LLL24:
                return new Lll24PosnSender(senderRpc, receiverParty, (Lll24PosnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PosnType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static PosnReceiver createReceiver(Rpc receiverRpc, Party senderParty, PosnConfig config) {
        PosnType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LLL24:
                return new Lll24PosnReceiver(receiverRpc, senderParty, (Lll24PosnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PosnType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PosnConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Lll24PosnConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

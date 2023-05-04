package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre.bea95.Bea95PreLnotSender;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT factory.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class PreLnotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PreLnotFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum PreLnotType {
        /**
         * Bea95
         */
        Bea95,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PreLnotSender createSender(Rpc senderRpc, Party receiverParty, PreLnotConfig config) {
        PreLnotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreLnotSender(senderRpc, receiverParty, (Bea95PreLnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PreLnotType.class.getSimpleName() + ": " + type.name());
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
    public static PreLnotReceiver createReceiver(Rpc receiverRpc, Party senderParty, PreLnotConfig config) {
        PreLnotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreLnotReceiver(receiverRpc, senderParty, (Bea95PreLnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PreLnotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PreLnotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Bea95PreLnotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

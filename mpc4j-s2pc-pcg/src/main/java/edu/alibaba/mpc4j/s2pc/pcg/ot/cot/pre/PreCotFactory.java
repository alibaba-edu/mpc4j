package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotSender;

/**
 * pre-compute COT factory.
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class PreCotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PreCotFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum PreCotType {
        /**
         * Bea95协议
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
    public static PreCotSender createSender(Rpc senderRpc, Party receiverParty, PreCotConfig config) {
        PreCotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreCotSender(senderRpc, receiverParty, (Bea95PreCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PreCotType.class.getSimpleName() + ": " + type.name());
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
    public static PreCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, PreCotConfig config) {
        PreCotType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case Bea95:
                return new Bea95PreCotReceiver(receiverRpc, senderParty, (Bea95PreCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + PreCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static PreCotConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Bea95PreCotConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

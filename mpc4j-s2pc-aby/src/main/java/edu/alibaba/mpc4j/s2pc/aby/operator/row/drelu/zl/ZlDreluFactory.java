package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluSender;

/**
 * Zl DReLU Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlDreluFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlDreluFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlDreluType {
        /**
         * RRK+20
         */
        RRK20,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlDreluParty createSender(Rpc senderRpc, Party receiverParty, ZlDreluConfig config) {
        ZlDreluFactory.ZlDreluType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlDreluSender(senderRpc, receiverParty, (Rrk20ZlDreluConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDreluType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDreluParty createReceiver(Rpc receiverRpc, Party senderParty, ZlDreluConfig config) {
        ZlDreluFactory.ZlDreluType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlDreluReceiver(receiverRpc, senderParty, (Rrk20ZlDreluConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDreluType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlDreluConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20ZlDreluConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

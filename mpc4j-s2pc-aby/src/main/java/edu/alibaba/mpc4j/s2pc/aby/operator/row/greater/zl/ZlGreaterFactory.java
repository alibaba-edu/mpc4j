package edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.rrk20.Rrk20ZlGreaterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.rrk20.Rrk20ZlGreaterReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.rrk20.Rrk20ZlGreaterSender;

/**
 * Zl Greater Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlGreaterFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlGreaterFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlGreaterType {
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
    public static ZlGreaterParty createSender(Rpc senderRpc, Party receiverParty, ZlGreaterConfig config) {
        ZlGreaterType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlGreaterSender(senderRpc, receiverParty, (Rrk20ZlGreaterConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlGreaterType.class.getSimpleName() + ": " + type.name());
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
    public static ZlGreaterParty createReceiver(Rpc receiverRpc, Party senderParty, ZlGreaterConfig config) {
        ZlGreaterType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlGreaterReceiver(receiverRpc, senderParty, (Rrk20ZlGreaterConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlGreaterType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlGreaterConfig createDefaultConfig(SecurityModel securityModel, boolean silent, Zl zl) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20ZlGreaterConfig.Builder(zl).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

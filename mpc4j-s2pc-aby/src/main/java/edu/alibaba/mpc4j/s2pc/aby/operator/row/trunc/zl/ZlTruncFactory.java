package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23.Gp23ZlTruncSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncSender;

/**
 * Zl Truncation Factory
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class ZlTruncFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlTruncFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlTruncType {
        /**
         * RRK+20
         */
        RRK20,
        /**
         * GP23
         */
        GP23,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlTruncParty createSender(Rpc senderRpc, Party receiverParty, ZlTruncConfig config) {
        ZlTruncType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlTruncSender(senderRpc, receiverParty, (Rrk20ZlTruncConfig) config);
            case GP23:
                return new Gp23ZlTruncSender(senderRpc, receiverParty, (Gp23ZlTruncConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlTruncType.class.getSimpleName() + ": " + type.name());
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
    public static ZlTruncParty createReceiver(Rpc receiverRpc, Party senderParty, ZlTruncConfig config) {
        ZlTruncType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlTruncReceiver(receiverRpc, senderParty, (Rrk20ZlTruncConfig) config);
            case GP23:
                return new Gp23ZlTruncReceiver(receiverRpc, senderParty, (Gp23ZlTruncConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlTruncType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlTruncConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20ZlTruncConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

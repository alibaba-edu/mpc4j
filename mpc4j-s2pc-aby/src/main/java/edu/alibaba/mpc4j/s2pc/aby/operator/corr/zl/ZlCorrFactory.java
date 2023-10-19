package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrSender;

/**
 * Zl Corr Factory
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class ZlCorrFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlCorrFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlCorrType {
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
    public static ZlCorrParty createSender(Rpc senderRpc, Party receiverParty, ZlCorrConfig config) {
        ZlCorrType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlCorrSender(senderRpc, receiverParty, (Rrk20ZlCorrConfig) config);
            case GP23:
                return new Gp23ZlCorrSender(senderRpc, receiverParty, (Gp23ZlCorrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCorrType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCorrParty createReceiver(Rpc receiverRpc, Party senderParty, ZlCorrConfig config) {
        ZlCorrType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlCorrReceiver(receiverRpc, senderParty, (Rrk20ZlCorrConfig) config);
            case GP23:
                return new Gp23ZlCorrReceiver(receiverRpc, senderParty, (Gp23ZlCorrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCorrType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlCorrConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20ZlCorrConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

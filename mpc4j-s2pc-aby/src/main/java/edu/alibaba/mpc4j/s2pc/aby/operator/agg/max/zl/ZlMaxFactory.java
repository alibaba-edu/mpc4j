package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxSender;

/**
 * Zl Max Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlMaxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlMaxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlMaxType {
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
    public static ZlMaxParty createSender(Rpc senderRpc, Party receiverParty, ZlMaxConfig config) {
        ZlMaxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMaxSender(senderRpc, receiverParty, (Rrk20ZlMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMaxType.class.getSimpleName() + ": " + type.name());
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
    public static ZlMaxParty createReceiver(Rpc receiverRpc, Party senderParty, ZlMaxConfig config) {
        ZlMaxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMaxReceiver(receiverRpc, senderParty, (Rrk20ZlMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMaxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlMaxConfig createDefaultConfig(SecurityModel securityModel, boolean silent, Zl zl) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Rrk20ZlMaxConfig.Builder(zl).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

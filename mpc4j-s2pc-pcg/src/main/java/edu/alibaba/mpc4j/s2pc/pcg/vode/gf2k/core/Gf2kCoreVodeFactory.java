package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24.Aprr24Gf2kCoreVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24.Aprr24Gf2kCoreVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.core.aprr24.Aprr24Gf2kCoreVodeSender;

/**
 * GF2K-core-VODE factory.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class Gf2kCoreVodeFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kCoreVodeFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum Gf2kCoreVodeType {
        /**
         * APRR24 (semi-honest)
         */
        APRR24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Gf2kCoreVodeSender createSender(Rpc senderRpc, Party receiverParty, Gf2kCoreVodeConfig config) {
        Gf2kCoreVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24Gf2kCoreVodeSender(senderRpc, receiverParty, (Aprr24Gf2kCoreVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kCoreVodeReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kCoreVodeConfig config) {
        Gf2kCoreVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24Gf2kCoreVodeReceiver(receiverRpc, senderParty, (Aprr24Gf2kCoreVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kCoreVodeType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the default config.
     *
     * @param securityModel security model.
     * @return the default config.
     */
    public static Gf2kCoreVodeConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Aprr24Gf2kCoreVodeConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

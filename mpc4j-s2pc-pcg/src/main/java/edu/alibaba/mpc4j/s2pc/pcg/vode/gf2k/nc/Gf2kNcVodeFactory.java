package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodeReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24.Aprr24Gf2kNcVodeSender;

/**
 * GF2K-NC-VODE factory.
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
public class Gf2kNcVodeFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kNcVodeFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum Gf2kNcVodeType {
        /**
         * APRR24
         */
        APRR24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Gf2kNcVodeSender createSender(Rpc senderRpc, Party receiverParty, Gf2kNcVodeConfig config) {
        Gf2kNcVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24Gf2kNcVodeSender(senderRpc, receiverParty, (Aprr24Gf2kNcVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kNcVodeType.class.getSimpleName() + ": " + type.name());
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
    public static Gf2kNcVodeReceiver createReceiver(Rpc receiverRpc, Party senderParty, Gf2kNcVodeConfig config) {
        Gf2kNcVodeType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24Gf2kNcVodeReceiver(receiverRpc, senderParty, (Aprr24Gf2kNcVodeConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kNcVodeType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static Gf2kNcVodeConfig createDefaultConfig(SecurityModel securityModel) {
        return new Aprr24Gf2kNcVodeConfig.Builder(securityModel).build();
    }
}

package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F32SowOprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;

/**
 * (F3, F2)-sowOPRF factory.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public class F32SowOprfFactory implements PtoFactory {
    /**
     * private constructor
     */
    private F32SowOprfFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum F32SowOprfType {
        /**
         * APRR24
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
    public static F32SowOprfSender createSender(Rpc senderRpc, Party receiverParty, F32SowOprfConfig config) {
        F32SowOprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24F32SowOprfSender(senderRpc, receiverParty, (Aprr24F32SowOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + F32SowOprfType.class.getSimpleName() + ": " + type.name());
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
    public static F32SowOprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, F32SowOprfConfig config) {
        F32SowOprfType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case APRR24:
                return new Aprr24F32SowOprfReceiver(receiverRpc, senderParty, (Aprr24F32SowOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + F32SowOprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param conv32Type Conv32 type.
     * @return a default config.
     */
    public static F32SowOprfConfig createDefaultConfig(Conv32Type conv32Type) {
        return new Aprr24F32SowOprfConfig.Builder(conv32Type).build();
    }
}

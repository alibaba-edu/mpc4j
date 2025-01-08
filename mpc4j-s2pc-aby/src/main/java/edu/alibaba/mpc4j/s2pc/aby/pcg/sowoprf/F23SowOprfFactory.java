package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24.Aprr24F23SowOprfSender;

/**
 * (F2, F3)-sowOPRF factory.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class F23SowOprfFactory implements PtoFactory {
    /**
     * private constructor
     */
    private F23SowOprfFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum F23SowOprfType {
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
    public static F23SowOprfSender createSender(Rpc senderRpc, Party receiverParty, F23SowOprfConfig config) {
        F23SowOprfType type = config.getPtoType();
        return switch (type) {
            case APRR24 -> new Aprr24F23SowOprfSender(senderRpc, receiverParty, (Aprr24F23SowOprfConfig) config);
        };
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static F23SowOprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, F23SowOprfConfig config) {
        F23SowOprfType type = config.getPtoType();
        return switch (type) {
            case APRR24 -> new Aprr24F23SowOprfReceiver(receiverRpc, senderParty, (Aprr24F23SowOprfConfig) config);
        };
    }

    /**
     * Creates a default config.
     *
     * @param silent using silent OT.
     * @return a default config.
     */
    public static F23SowOprfConfig createDefaultConfig(boolean silent) {
        return new Aprr24F23SowOprfConfig.Builder(silent).build();
    }

    /**
     * Gets pre-computed COT num.
     *
     * @param size size.
     * @return pre-computed COT num.
     */
    public static int getPreCotNum(int size) {
        MathPreconditions.checkPositive("size", size);
        return size * F23Wprf.M;
    }
}

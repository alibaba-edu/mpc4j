package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.lkz24.*;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24.Plg24ZlDaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24.Plg24ZlDaBitGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.dabit.zl.plg24.Plg24ZlDaBitGenSender;

/**
 * Zl daBit generation factory.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class ZlDaBitGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlDaBitGenFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ZlDaBitGenType {
        /**
         * fake
         */
        FAKE,
        /**
         * aided
         */
        AIDED,
        /**
         * LZK24
         */
        LZK24,
        /**
         * PLL24
         */
        PLG24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static ZlDaBitGenParty createSender(Rpc senderRpc, Party receiverParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        return switch (type) {
            case LZK24 -> new Lzk24ZlDaBitGenSender(senderRpc, receiverParty, (Lkz24ZlDaBitGenConfig) config);
            case PLG24 -> new Plg24ZlDaBitGenSender(senderRpc, receiverParty, (Plg24ZlDaBitGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDaBitGenParty createReceiver(Rpc receiverRpc, Party senderParty, ZlDaBitGenConfig config) {
        ZlDaBitGenType type = config.getPtoType();
        return switch (type) {
            case LZK24 -> new Lzk24ZlDaBitGenReceiver(receiverRpc, senderParty, (Lkz24ZlDaBitGenConfig) config);
            case PLG24 -> new Plg24ZlDaBitGenReceiver(receiverRpc, senderParty, (Plg24ZlDaBitGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlDaBitGenType.class.getSimpleName() + ": " + type.name());
        };
    }
}

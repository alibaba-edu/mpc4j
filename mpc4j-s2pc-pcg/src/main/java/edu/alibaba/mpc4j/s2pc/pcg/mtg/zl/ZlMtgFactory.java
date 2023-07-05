package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache.CacheZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache.CacheZlMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache.CacheZlMtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline.OfflineZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline.OfflineZlMtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline.OfflineZlMtgSender;

/**
 * Zl multiplication triple generator factory.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlMtgFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlMtgFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlMtgType {
        /**
         * offline
         */
        OFFLINE,
        /**
         * cache
         */
        CACHE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static ZlMtgParty createSender(Rpc senderRpc, Party receiverParty, ZlMtgConfig config) {
        ZlMtgType type = config.getPtoType();
        switch (type) {
            case CACHE:
                return new CacheZlMtgSender(senderRpc, receiverParty, (CacheZlMtgConfig) config);
            case OFFLINE:
                return new OfflineZlMtgSender(senderRpc, receiverParty, (OfflineZlMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param aiderParty    aider party.
     * @param config        config.
     * @return a sender.
     */
    public static ZlMtgParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, ZlMtgConfig config) {
        ZlMtgType type = config.getPtoType();
        switch (type) {
            case CACHE:
                return new CacheZlMtgSender(senderRpc, receiverParty, aiderParty, (CacheZlMtgConfig) config);
            case OFFLINE:
                return new OfflineZlMtgSender(senderRpc, receiverParty, aiderParty, (OfflineZlMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMtgType.class.getSimpleName() + ": " + type.name());
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
    public static ZlMtgParty createReceiver(Rpc receiverRpc, Party senderParty, ZlMtgConfig config) {
        ZlMtgType type = config.getPtoType();
        switch (type) {
            case CACHE:
                return new CacheZlMtgReceiver(receiverRpc, senderParty, (CacheZlMtgConfig) config);
            case OFFLINE:
                return new OfflineZlMtgReceiver(receiverRpc, senderParty, (OfflineZlMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a receiver.
     */
    public static ZlMtgParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, ZlMtgConfig config) {
        ZlMtgType type = config.getPtoType();
        switch (type) {
            case CACHE:
                return new CacheZlMtgReceiver(receiverRpc, senderParty, aiderParty, (CacheZlMtgConfig) config);
            case OFFLINE:
                return new OfflineZlMtgReceiver(receiverRpc, senderParty, aiderParty, (OfflineZlMtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @return a default config.
     */
    public static ZlMtgConfig createDefaultConfig(SecurityModel securityModel, Zl zl) {
        return new CacheZlMtgConfig.Builder(securityModel, zl).build();
    }
}

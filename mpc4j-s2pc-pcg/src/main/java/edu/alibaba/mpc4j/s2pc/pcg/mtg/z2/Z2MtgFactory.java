package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.cache.CacheZ2MtgSender;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline.OfflineZ2MtgSender;

/**
 * Zl multiplication triple generator factory.
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class Z2MtgFactory implements PtoFactory {
    /**
     * private constructor
     */
    private Z2MtgFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Z2MtgType {
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
    public static Z2MtgParty createSender(Rpc senderRpc, Party receiverParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case OFFLINE:
                return new OfflineZ2MtgSender(senderRpc, receiverParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgSender(senderRpc, receiverParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2MtgParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case OFFLINE:
                return new OfflineZ2MtgSender(senderRpc, receiverParty, aiderParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgSender(senderRpc, receiverParty, aiderParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2MtgParty createReceiver(Rpc receiverRpc, Party senderParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case OFFLINE:
                return new OfflineZ2MtgReceiver(receiverRpc, senderParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgReceiver(receiverRpc, senderParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
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
    public static Z2MtgParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Z2MtgConfig config) {
        Z2MtgType type = config.getPtoType();
        switch (type) {
            case OFFLINE:
                return new OfflineZ2MtgReceiver(receiverRpc, senderParty, aiderParty, (OfflineZ2MtgConfig) config);
            case CACHE:
                return new CacheZ2MtgReceiver(receiverRpc, senderParty, aiderParty, (CacheZ2MtgConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static Z2MtgConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new CacheZ2MtgConfig.Builder(securityModel)
            .setCoreMtgConfig(Z2CoreMtgFactory.createDefaultConfig(securityModel, silent))
            .build();
    }
}

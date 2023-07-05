package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc.LowMcOprpSender;

/**
 * OPRP factory.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class OprpFactory implements PtoFactory {
    /**
     * private constructor
     */
    private OprpFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum OprpType {
        /**
         * LowMC
         */
        LOW_MC,
        /**
         * inverse LowMC
         */
        LOW_MC_INV,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static OprpSender createSender(Rpc senderRpc, Party receiverParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpSender(senderRpc, receiverParty, (LowMcOprpConfig) config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
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
    public static OprpSender createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpSender(senderRpc, receiverParty, aiderParty, (LowMcOprpConfig) config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
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
    public static OprpReceiver createReceiver(Rpc receiverRpc, Party senderParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpReceiver(receiverRpc, senderParty, (LowMcOprpConfig) config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
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
    public static OprpReceiver createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, OprpConfig config) {
        OprpType type = config.getPtoType();
        switch (type) {
            case LOW_MC:
                return new LowMcOprpReceiver(receiverRpc, senderParty, aiderParty, (LowMcOprpConfig) config);
            case LOW_MC_INV:
            default:
                throw new IllegalArgumentException("Invalid " + OprpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        use silent.
     * @return a default config.
     */
    public static OprpConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new LowMcOprpConfig.Builder(securityModel)
            .setZ2cConfig(Z2cFactory.createDefaultConfig(securityModel, silent))
            .build();
    }
}

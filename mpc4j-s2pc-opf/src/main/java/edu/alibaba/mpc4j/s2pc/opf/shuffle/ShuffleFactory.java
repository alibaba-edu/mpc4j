package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20.Cgp20ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20.Cgp20ShuffleReceiver;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20.Cgp20ShuffleSender;

/**
 * shuffle factory
 *
 * @author Feng Han
 * @date 2024/9/27
 */
public class ShuffleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ShuffleFactory() {
        // empty
    }

    /**
     * type
     */
    public enum ShuffleType {
        /**
         * CGP20
         */
        CGP20,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ShuffleParty createSender(Rpc senderRpc, Party receiverParty, ShuffleConfig config) {
        ShuffleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGP20:
                return new Cgp20ShuffleSender(senderRpc, receiverParty, (Cgp20ShuffleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShuffleType.class.getSimpleName() + ": " + type.name());
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
    public static ShuffleParty createReceiver(Rpc receiverRpc, Party senderParty, ShuffleConfig config) {
        ShuffleType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGP20:
                return new Cgp20ShuffleReceiver(receiverRpc, senderParty, (Cgp20ShuffleConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ShuffleType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param silent if using a silent protocol.
     * @return a default config.
     */
    public static ShuffleConfig createDefaultConfig(boolean silent) {
        return new Cgp20ShuffleConfig.Builder(silent).build();
    }
}

package edu.alibaba.mpc4j.s2pc.opf.osorter;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic.BitonicSorter;
import edu.alibaba.mpc4j.s2pc.opf.osorter.bitonic.BitonicSorterConfig;
import edu.alibaba.mpc4j.s2pc.opf.osorter.quick.QuickSorter;
import edu.alibaba.mpc4j.s2pc.opf.osorter.quick.QuickSorterConfig;

/**
 * ObSorter factory
 *
 * @author Feng Han
 * @date 2024/9/27
 */
public class ObSortFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ObSortFactory() {
        // empty
    }

    /**
     * type
     */
    public enum ObSortType {
        /**
         * Quick
         */
        QUICK,
        /**
         * bitonic
         */
        BITONIC,

    }

    /**
     * Creates a sender.
     *
     * @param ownRpc     the own RPC.
     * @param otherParty the other party.
     * @param config        the config.
     * @return a sender.
     */
    public static ObSorter createSorter(Rpc ownRpc, Party otherParty, ObSortConfig config) {
        ObSortType type = config.getPtoType();
        switch (type) {
            case QUICK:
                return new QuickSorter(ownRpc, otherParty, (QuickSorterConfig) config);
            case BITONIC:
                return new BitonicSorter(ownRpc, otherParty, (BitonicSorterConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ObSortType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param silent if using a silent protocol.
     * @return a default config.
     */
    public static ObSortConfig createDefaultConfig(boolean silent) {
        return new QuickSorterConfig.Builder(silent).build();
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent.SilentCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent.SilentCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.silent.SilentCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotSender;

/**
 * COT factory.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CotFactory implements PtoFactory {
    /**
     * private constructor
     */
    private CotFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum CotType {
        /**
         * directly invoke OT extension
         */
        DIRECT,
        /**
         * silent COT
         */
        SILENT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static CotSender createSender(Rpc senderRpc, Party receiverParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCotSender(senderRpc, receiverParty, (DirectCotConfig) config);
            case SILENT:
                return new SilentCotSender(senderRpc, receiverParty, (SilentCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
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
    public static CotReceiver createReceiver(Rpc receiverRpc, Party senderParty, CotConfig config) {
        CotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCotReceiver(receiverRpc, senderParty, (DirectCotConfig) config);
            case SILENT:
                return new SilentCotReceiver(receiverRpc, senderParty, (SilentCotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static CotConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        if (silent) {
            return new SilentCotConfig.Builder(securityModel).build();
        } else {
            return new DirectCotConfig.Builder(securityModel).build();
        }
    }
}

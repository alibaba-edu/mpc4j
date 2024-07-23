package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot.CotLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot.CotLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.cot.CotLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct.DirectLnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct.DirectLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct.DirectLnotSender;

/**
 * 1-ouf-of n (with n = 2^l) OT factory.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class LnotFactory implements PtoFactory {
    /**
     * private constructor
     */
    private LnotFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum LnotType {
        /**
         * directly invoke 1-out-of-2^l COT
         */
        DIRECT,
        /**
         * Cache LNOT
         */
        COT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static LnotSender createSender(Rpc senderRpc, Party receiverParty, LnotConfig config) {
        LnotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectLnotSender(senderRpc, receiverParty, (DirectLnotConfig) config);
            case COT:
                return new CotLnotSender(senderRpc, receiverParty, (CotLnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + LnotType.class.getSimpleName() + ": " + type.name());
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
    public static LnotReceiver createReceiver(Rpc receiverRpc, Party senderParty, LnotConfig config) {
        LnotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectLnotReceiver(receiverRpc, senderParty, (DirectLnotConfig) config);
            case COT:
                return new CotLnotReceiver(receiverRpc, senderParty, (CotLnotConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + LnotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates default config.
     *
     * @param securityModel security model.
     * @param silent if using silent OT.
     * @return the config.
     */
    public static LnotConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        if (silent) {
            return new CotLnotConfig.Builder(securityModel).build();
        } else {
            return new DirectLnotConfig.Builder(securityModel).build();
        }
    }
}

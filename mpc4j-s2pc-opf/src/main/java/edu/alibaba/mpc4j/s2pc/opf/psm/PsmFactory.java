package edu.alibaba.mpc4j.s2pc.opf.psm;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.cgs22.*;

/**
 * private set membership factory.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class PsmFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PsmFactory() {
        // empty
    }

    /**
     * type
     */
    public enum PsmType {
        /**
         * CGS22 based on LNOT
         */
        CGS22_LNOT,
        /**
         * CGS22 based on OPPRF (PSM2)
         */
        CGS22_OPPRF,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PsmSender createSender(Rpc senderRpc, Party receiverParty, PsmConfig config) {
        PsmType type = config.getPtoType();
        switch (type) {
            case CGS22_LNOT:
                return new Cgs22LnotPsmSender(senderRpc, receiverParty, (Cgs22LnotPsmConfig) config);
            case CGS22_OPPRF:
                return new Cgs22OpprfPsmSender(senderRpc, receiverParty, (Cgs22OpprfPsmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsmType.class.getSimpleName() + ": " + type.name());
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
    public static PsmReceiver createReceiver(Rpc receiverRpc, Party senderParty, PsmConfig config) {
        PsmType type = config.getPtoType();
        switch (type) {
            case CGS22_LNOT:
                return new Cgs22LnotPsmReceiver(receiverRpc, senderParty, (Cgs22LnotPsmConfig) config);
            case CGS22_OPPRF:
                return new Cgs22OpprfPsmReceiver(receiverRpc, senderParty, (Cgs22OpprfPsmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsmType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PsmConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Cgs22OpprfPsmConfig.Builder(securityModel, silent).build();
    }
}

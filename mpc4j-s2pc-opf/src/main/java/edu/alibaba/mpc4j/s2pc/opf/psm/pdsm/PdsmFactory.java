package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.cgs22.*;

/**
 * private (distinct) set membership factory.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class PdsmFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PdsmFactory() {
        // empty
    }

    /**
     * type
     */
    public enum PdsmType {
        /**
         * CGS22 naive PSM (PSM1)
         */
        CGS22_NAIVE,
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
    public static PdsmSender createSender(Rpc senderRpc, Party receiverParty, PdsmConfig config) {
        PdsmType type = config.getPtoType();
        switch (type) {
            case CGS22_NAIVE:
                return new Cgs22NaivePdsmSender(senderRpc, receiverParty, (Cgs22NaivePdsmConfig) config);
            case CGS22_OPPRF:
                return new Cgs22OpprfPdsmSender(senderRpc, receiverParty, (Cgs22OpprfPdsmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PdsmType.class.getSimpleName() + ": " + type.name());
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
    public static PdsmReceiver createReceiver(Rpc receiverRpc, Party senderParty, PdsmConfig config) {
        PdsmType type = config.getPtoType();
        switch (type) {
            case CGS22_NAIVE:
                return new Cgs22NaivePdsmReceiver(receiverRpc, senderParty, (Cgs22NaivePdsmConfig) config);
            case CGS22_OPPRF:
                return new Cgs22OpprfPdsmReceiver(receiverRpc, senderParty, (Cgs22OpprfPdsmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PdsmType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PdsmConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Cgs22OpprfPdsmConfig.Builder(securityModel, silent).build();
    }
}

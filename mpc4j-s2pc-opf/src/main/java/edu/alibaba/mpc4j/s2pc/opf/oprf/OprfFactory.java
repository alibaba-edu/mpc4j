package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.fipr05.Fipr05MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16.*;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.rs21.Rs21MpOprfSender;

/**
 * OPRF factory.
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class OprfFactory implements PtoFactory {
    /**
     * private constructor
     */
    private OprfFactory() {
        // empty
    }

    /**
     * type
     */
    public enum OprfType {
        /**
         * FIPR05
         */
        FIPR05,
        /**
         * optimized KKRT16
         */
        KKRT16_OPT,
        /**
         * original KKRT16
         */
        KKRT16_ORI,
        /**
         * CM20
         */
        CM20,
        /**
         * RS21
         */
        RS21,
    }

    /**
     * Creates an OPRF sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static OprfSender createOprfSender(Rpc senderRpc, Party receiverParty, OprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case FIPR05:
                return new Fipr05MpOprfSender(senderRpc, receiverParty, (Fipr05MpOprfConfig) config);
            case KKRT16_ORI:
                return new Kkrt16OriOprfSender(senderRpc, receiverParty, (Kkrt16OriOprfConfig) config);
            case KKRT16_OPT:
                return new Kkrt16OptOprfSender(senderRpc, receiverParty, (Kkrt16OptOprfConfig) config);
            case CM20:
                return new Cm20MpOprfSender(senderRpc, receiverParty, (Cm20MpOprfConfig) config);
            case RS21:
                return new Rs21MpOprfSender(senderRpc, receiverParty, (Rs21MpOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an OPRF receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static OprfReceiver createOprfReceiver(Rpc receiverRpc, Party senderParty, OprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case FIPR05:
                return new Fipr05MpOprfReceiver(receiverRpc, senderParty, (Fipr05MpOprfConfig) config);
            case KKRT16_ORI:
                return new Kkrt16OriOprfReceiver(receiverRpc, senderParty, (Kkrt16OriOprfConfig) config);
            case KKRT16_OPT:
                return new Kkrt16OptOprfReceiver(receiverRpc, senderParty, (Kkrt16OptOprfConfig) config);
            case CM20:
                return new Cm20MpOprfReceiver(receiverRpc, senderParty, (Cm20MpOprfConfig) config);
            case RS21:
                return new Rs21MpOprfReceiver(receiverRpc, senderParty, (Rs21MpOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default OPRF config.
     *
     * @param securityModel the security model.
     * @return a default OPRF config.
     */
    public static OprfConfig createOprfDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Kkrt16OptOprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }

    /**
     * Creates a multi-query OPRF sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static MpOprfSender createMpOprfSender(Rpc senderRpc, Party receiverParty, MpOprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case FIPR05:
                return new Fipr05MpOprfSender(senderRpc, receiverParty, (Fipr05MpOprfConfig) config);
            case CM20:
                return new Cm20MpOprfSender(senderRpc, receiverParty, (Cm20MpOprfConfig) config);
            case RS21:
                return new Rs21MpOprfSender(senderRpc, receiverParty, (Rs21MpOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a multi-query OPRF receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static MpOprfReceiver createMpOprfReceiver(Rpc receiverRpc, Party senderParty, MpOprfConfig config) {
        OprfType type = config.getPtoType();
        switch (type) {
            case FIPR05:
                return new Fipr05MpOprfReceiver(receiverRpc, senderParty, (Fipr05MpOprfConfig) config);
            case CM20:
                return new Cm20MpOprfReceiver(receiverRpc, senderParty, (Cm20MpOprfConfig) config);
            case RS21:
                return new Rs21MpOprfReceiver(receiverRpc, senderParty, (Rs21MpOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + OprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default multi-query OPRF config.
     *
     * @param securityModel the security model.
     * @return a default multi-query OPRF config.
     */
    public static MpOprfConfig createMpOprfDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Cm20MpOprfConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23.*;

/**
 * Permuted Matrix Private Equality Test factory.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public class PmPeqtFactory implements PtoFactory {

    /**
     * private constructor.
     */
    private PmPeqtFactory() {
        // empty
    }

    /**
     * pm-PEQT type
     */
    public enum PmPeqtType {
        /**
         * TCL23 (Permute Share + OPRF)
         */
        TZL23_PS_OPRF,
        /**
         * TCL23 (Byte Ecc DDH)
         */
        TCL23_BYTE_ECC_DDH,
        /**
         * TCL23 (Ecc DDH)
         */
        TCL23_ECC_DDH,
    }

    /**
     * Create a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static PmPeqtSender createSender(Rpc senderRpc, Party receiverParty, PmPeqtConfig config) {
        PmPeqtType type = config.getPtoType();
        switch (type) {
            case TZL23_PS_OPRF:
                return new Tcl23PsOprfPmPeqtSender(senderRpc, receiverParty, (Tcl23PsOprfPmPeqtConfig) config);
            case TCL23_BYTE_ECC_DDH:
                return new Tcl23ByteEccDdhPmPeqtSender(senderRpc, receiverParty, (Tcl23ByteEccDdhPmPeqtConfig) config);
            case TCL23_ECC_DDH:
                return new Tcl23EccDdhPmPeqtSender(senderRpc, receiverParty, (Tcl23EccDdhPmPeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmPeqtType.class.getSimpleName() + ": " + type.name());
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
    public static PmPeqtReceiver createReceiver(Rpc receiverRpc, Party senderParty, PmPeqtConfig config) {
        PmPeqtType type = config.getPtoType();
        switch (type) {
            case TZL23_PS_OPRF:
                return new Tcl23PsOprfPmPeqtReceiver(receiverRpc, senderParty, (Tcl23PsOprfPmPeqtConfig) config);
            case TCL23_BYTE_ECC_DDH:
                return new Tcl23ByteEccDdhPmPeqtReceiver(receiverRpc, senderParty, (Tcl23ByteEccDdhPmPeqtConfig) config);
            case TCL23_ECC_DDH:
                return new Tcl23EccDdhPmPeqtReceiver(receiverRpc, senderParty, (Tcl23EccDdhPmPeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmPeqtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default pm-PEQT config.
     *
     * @param securityModel the security model.
     * @return a default pm-PEQT config.
     */
    public static PmPeqtConfig createPmPeqtDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Tcl23ByteEccDdhPmPeqtConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
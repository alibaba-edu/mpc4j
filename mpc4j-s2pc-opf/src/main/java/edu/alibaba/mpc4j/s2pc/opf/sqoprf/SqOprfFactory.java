package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09.Pssw09SqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04.Nr04EccSqOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.*;

/**
 * single-query OPRF factory.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SqOprfFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum SqOprfType {
        /**
         * RA17 based on ECC
         */
        RA17_ECC,
        /**
         * RA17 based on byte ECC
         */
        RA17_BYTE_ECC,
        /**
         * Naor-Reingold OPRF based on ECC
         */
        NR04_ECC,
        /**
         * LowMC OPRF
         */
        PSSW09,
    }

    /**
     * Creates the sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return the sender.
     */
    public static SqOprfSender createSender(Rpc senderRpc, Party receiverParty, SqOprfConfig config) {
        SqOprfType type = config.getPtoType();
        switch (type) {
            case RA17_ECC:
                return new Ra17EccSqOprfSender(senderRpc, receiverParty, (Ra17EccSqOprfConfig) config);
            case RA17_BYTE_ECC:
                return new Ra17ByteEccSqOprfSender(senderRpc, receiverParty, (Ra17ByteEccSqOprfConfig) config);
            case NR04_ECC:
                return new Nr04EccSqOprfSender(senderRpc, receiverParty, (Nr04EccSqOprfConfig) config);
            case PSSW09:
                return new Pssw09SqOprfSender(senderRpc, receiverParty, (Pssw09SqOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SqOprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return the receiver.
     */
    public static SqOprfReceiver createReceiver(Rpc receiverRpc, Party senderParty, SqOprfConfig config) {
        SqOprfType type = config.getPtoType();
        switch (type) {
            case RA17_ECC:
                return new Ra17EccSqOprfReceiver(receiverRpc, senderParty, (Ra17EccSqOprfConfig) config);
            case RA17_BYTE_ECC:
                return new Ra17ByteEccSqOprfReceiver(receiverRpc, senderParty, (Ra17ByteEccSqOprfConfig) config);
            case NR04_ECC:
                return new Nr04EccSqOprfReceiver(receiverRpc, senderParty, (Nr04EccSqOprfConfig) config);
            case PSSW09:
                return new Pssw09SqOprfReceiver(receiverRpc, senderParty, (Pssw09SqOprfConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + SqOprfType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates the default config.
     *
     * @param securityModel the security model.
     * @return 默认协议配置项。
     */
    public static SqOprfConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Ra17ByteEccSqOprfConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

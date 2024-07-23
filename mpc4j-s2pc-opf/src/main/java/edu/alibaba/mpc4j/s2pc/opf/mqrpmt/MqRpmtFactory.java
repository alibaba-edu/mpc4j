package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtServer;

/**
 * mqRPMT Factory.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class MqRpmtFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private MqRpmtFactory() {
        // empty
    }

    /**
     * mqRPMT type
     */
    public enum MqRpmtType {
        /**
         * GMR21
         */
        GMR21,
        /**
         * JSZ22 (shuffle client)
         */
        JSZ22_SFC,
        /**
         * ZCL23 (PKE)
         */
        ZCL23_PKE,
        /**
         * CZZ22_BYTE_ECC_CW_PRF
         */
        CZZ24_CW_OPRF,
    }

    /**
     * Create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static MqRpmtServer createServer(Rpc serverRpc, Party clientParty, MqRpmtConfig config) {
        MqRpmtType type = config.getPtoType();
        switch (type) {
            case GMR21:
                return new Gmr21MqRpmtServer(serverRpc, clientParty, (Gmr21MqRpmtConfig) config);
            case ZCL23_PKE:
                return new Zcl23PkeMqRpmtServer(serverRpc, clientParty, (Zcl23PkeMqRpmtConfig) config);
            case CZZ24_CW_OPRF:
                return new Czz24CwOprfMqRpmtServer(serverRpc, clientParty, (Czz24CwOprfMqRpmtConfig) config);
            case JSZ22_SFC:
            default:
                throw new IllegalArgumentException("Invalid " + MqRpmtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static MqRpmtClient createClient(Rpc clientRpc, Party serverParty, MqRpmtConfig config) {
        MqRpmtType type = config.getPtoType();
        switch (type) {
            case GMR21:
                return new Gmr21MqRpmtClient(clientRpc, serverParty, (Gmr21MqRpmtConfig) config);
            case ZCL23_PKE:
                return new Zcl23PkeMqRpmtClient(clientRpc, serverParty, (Zcl23PkeMqRpmtConfig) config);
            case CZZ24_CW_OPRF:
                return new Czz24CwOprfMqRpmtClient(clientRpc, serverParty, (Czz24CwOprfMqRpmtConfig) config);
            case JSZ22_SFC:
            default:
                throw new IllegalArgumentException("Invalid " + MqRpmtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates default mqRPMT config.
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static MqRpmtConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Czz24CwOprfMqRpmtConfig.Builder().build();
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

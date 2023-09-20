package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtServer;

/**
 * Multi-Query RPMT factory.
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
     * mq-RPMT type
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
         * ZCL22 (PKE)
         */
        ZCL22_PKE,
        /**
         * ZCL22 (SKE)
         */
        ZCL22_SKE,
        /**
         * CZZ22_BYTE_ECC_CW_PRF
         */
        CZZ22_BYTE_ECC_CW,
        /**
         * CZZ22_ECC_CW_PRF
         */
        CZZ22_ECC_CW,
        /**
         * CZZ22_ECC_PO_PRF
         */
        CZZ22_ECC_PO,
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
            case CZZ22_BYTE_ECC_CW:
                return new Czz22ByteEccCwMqRpmtServer(serverRpc, clientParty, (Czz22ByteEccCwMqRpmtConfig) config);
            case ZCL22_SKE:
            case ZCL22_PKE:
            case JSZ22_SFC:
            case CZZ22_ECC_CW:
            case CZZ22_ECC_PO:
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
            case CZZ22_BYTE_ECC_CW:
                return new Czz22ByteEccCwMqRpmtClient(clientRpc, serverParty, (Czz22ByteEccCwMqRpmtConfig) config);
            case ZCL22_SKE:
            case ZCL22_PKE:
            case JSZ22_SFC:
            case CZZ22_ECC_CW:
            case CZZ22_ECC_PO:
            default:
                throw new IllegalArgumentException("Invalid " + MqRpmtType.class.getSimpleName() + ": " + type.name());
        }
    }
}

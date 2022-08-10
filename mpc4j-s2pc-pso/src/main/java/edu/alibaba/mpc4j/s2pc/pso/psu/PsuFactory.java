package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.*;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.*;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.*;

/**
 * PSU协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class PsuFactory {
    /**
     * 私有构造函数
     */
    private PsuFactory() {
        // empty
    }

    /**
     * PSU协议类型。
     */
    public enum PsuType {
        /**
         * KRTW19原始方案
         */
        KRTW19_ORI,
        /**
         * KRTW19优化方案
         */
        KRTW19_OPT,
        /**
         * GMR21方案
         */
        GMR21,
        /**
         * JSZ22置乱客户端方案
         */
        JSZ22_SFC,
        /**
         * JSZ22置乱服务端方案
         */
        JSZ22_SFS,
        /**
         * ZCL22_PKE方案
         */
        ZCL22_PKE,
        /**
         * ZCL22_SKE方案
         */
        ZCL22_SKE,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static PsuServer createServer(Rpc serverRpc, Party clientParty, PsuConfig config) {
        PsuType type = config.getPtoType();
        switch (type) {
            case KRTW19_ORI:
                return new Krtw19OriPsuServer(serverRpc, clientParty, (Krtw19OriPsuConfig)config);
            case KRTW19_OPT:
                return new Krtw19OptPsuServer(serverRpc, clientParty, (Krtw19OptPsuConfig)config);
            case GMR21:
                return new Gmr21PsuServer(serverRpc, clientParty, (Gmr21PsuConfig)config);
            case ZCL22_SKE:
                return new Zcl22SkePsuServer(serverRpc, clientParty, (Zcl22SkePsuConfig)config);
            case ZCL22_PKE:
                return new Zcl22PkePsuServer(serverRpc, clientParty, (Zcl22PkePsuConfig)config);
            case JSZ22_SFC:
                return new Jsz22SfcPsuServer(serverRpc, clientParty, (Jsz22SfcPsuConfig)config);
            case JSZ22_SFS:
                return new Jsz22SfsPsuServer(serverRpc, clientParty, (Jsz22SfsPsuConfig)config);
            default:
                throw new IllegalArgumentException("Invalid PsuType: " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc 客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static PsuClient createClient(Rpc clientRpc, Party serverParty, PsuConfig config) {
        PsuType type = config.getPtoType();
        switch (type) {
            case KRTW19_ORI:
                return new Krtw19OriPsuClient(clientRpc, serverParty, (Krtw19OriPsuConfig)config);
            case KRTW19_OPT:
                return new Krtw19OptPsuClient(clientRpc, serverParty, (Krtw19OptPsuConfig)config);
            case GMR21:
                return new Gmr21PsuClient(clientRpc, serverParty, (Gmr21PsuConfig)config);
            case ZCL22_SKE:
                return new Zcl22SkePsuClient(clientRpc, serverParty, (Zcl22SkePsuConfig)config);
            case ZCL22_PKE:
                return new Zcl22PkePsuClient(clientRpc, serverParty, (Zcl22PkePsuConfig)config);
            case JSZ22_SFC:
                return new Jsz22SfcPsuClient(clientRpc, serverParty, (Jsz22SfcPsuConfig)config);
            case JSZ22_SFS:
                return new Jsz22SfsPsuClient(clientRpc, serverParty, (Jsz22SfsPsuConfig)config);
            default:
                throw new IllegalArgumentException("Invalid PsuType: " + type.name());
        }
    }

    public static PsuConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Gmr21PsuConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid SecurityModel: " + securityModel.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirServer;

/**
 * 关键词索引PIR协议工厂。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class KwPirFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private KwPirFactory() {
        // empty
    }

    /**
     * 关键词索引PIR协议类型。
     */
    public enum KwPirType {
        /**
         * CMG21
         */
        CMG21,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <T> KwPirServer<T> createServer(Rpc serverRpc, Party clientParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21KwPirServer<>(serverRpc, clientParty, (Cmg21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static <T> KwPirClient<T> createClient(Rpc clientRpc, Party serverParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21KwPirClient<>(clientRpc, serverParty, (Cmg21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static KwPirConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Cmg21KwPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

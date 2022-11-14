package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiServer;

/**
 * 非平衡PSI协议工厂。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class UpsiFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private UpsiFactory() {
        // empty
    }

    /**
     * PSU协议类型。
     */
    public enum UpsiType {
        /**
         * CMG21方案
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
    public static <T> UpsiServer<T> createServer(Rpc serverRpc, Party clientParty, UpsiConfig config) {
        UpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21UpsiServer<>(serverRpc, clientParty, (Cmg21UpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <T> UpsiClient<T> createClient(Rpc clientRpc, Party serverParty, UpsiConfig config) {
        UpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21UpsiClient<>(clientRpc, serverParty, (Cmg21UpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建默认配置项。
     *
     * @param securityModel 安全模型。
     * @return 默认配置项。
     */
    public static UpsiConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Cmg21UpsiConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}

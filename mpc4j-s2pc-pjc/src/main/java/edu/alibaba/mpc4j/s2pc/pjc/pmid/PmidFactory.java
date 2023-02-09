package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.*;

/**
 * PMID协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public class PmidFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private PmidFactory() {
        // empty
    }

    /**
     * PMID协议类型。
     */
    public enum PmidType {
        /**
         * 多点ZCL22协议
         */
        ZCL22_MP,
        /**
         * 稀疏ZCL22协议
         */
        ZCL22_SLOPPY,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @param <X>         集合类型。
     * @return 服务端。
     */
    public static <X> PmidServer<X> createServer(Rpc serverRpc, Party clientParty, PmidConfig config) {
        PmidType type = config.getPtoType();
        switch (type) {
            case ZCL22_MP:
                return new Zcl22MpPmidServer<>(serverRpc, clientParty, (Zcl22MpPmidConfig) config);
            case ZCL22_SLOPPY:
                return new Zcl22SloppyPmidServer<>(serverRpc, clientParty, (Zcl22SloppyPmidConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmidType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @param <X>         集合类型。
     * @return 客户端。
     */
    public static <X> PmidClient<X> createClient(Rpc clientRpc, Party serverParty, PmidConfig config) {
        PmidType type = config.getPtoType();
        switch (type) {
            case ZCL22_MP:
                return new Zcl22MpPmidClient<>(clientRpc, serverParty, (Zcl22MpPmidConfig) config);
            case ZCL22_SLOPPY:
                return new Zcl22SloppyPmidClient<>(clientRpc, serverParty, (Zcl22SloppyPmidConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmidType.class.getSimpleName() + ": " + type.name());
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiServer;

/**
 * PSI协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private PsiFactory() {
        // empty
    }

    /**
     * PSI协议类型。
     */
    public enum PsiType {
        /**
         * HFH99椭圆曲线方案
         */
        HFH99_ECC,
        /**
         * HFH99字节椭圆曲线方案
         */
        HFH99_BYTE_ECC,
        /**
         * KKRT16方案
         */
        KKRT16,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <X> PsiServer<X> createServer(Rpc serverRpc, Party clientParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiServer<>(serverRpc, clientParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiServer<>(serverRpc, clientParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiServer<>(serverRpc, clientParty, (Kkrt16PsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PsiClient<X> createClient(Rpc clientRpc, Party serverParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiClient<>(clientRpc, serverParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiClient<>(clientRpc, serverParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiClient<>(clientRpc, serverParty, (Kkrt16PsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}

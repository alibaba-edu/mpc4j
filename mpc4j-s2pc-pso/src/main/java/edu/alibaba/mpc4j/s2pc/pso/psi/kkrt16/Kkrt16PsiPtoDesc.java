package edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KKRT16-PSI协议信息。论文来源：
 * <p>
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection. CCS 2016, ACM, 2016, pp. 818-829.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class Kkrt16PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7043357406784082959L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KKRT16_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 服务端发送哈希桶PRF
         */
        SERVER_SEND_BIN_PRFS,
        /**
         * 服务端发送贮存区PRF
         */
        SERVER_SEND_STASH_PRFS,
    }

    /**
     * 单例模式
     */
    private static final Kkrt16PsiPtoDesc INSTANCE = new Kkrt16PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Kkrt16PsiPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}

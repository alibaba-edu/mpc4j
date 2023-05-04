package edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KKRT16-ORI-OPRF协议信息。此方案是原始KKRT16方案，未使用ALSZ13中给出的通信优化技术。论文来源：
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection. CCS 2016, ACM, 2016, pp. 818-829.
 *
 * @author Weiran Liu
 * @date 2022/02/05
 */
class Kkrt16OriOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)8132740185933254760L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KKRT16_ORI_OPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送伪随机编码密钥
         */
        RECEIVER_SEND_KEY,
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * 单例模式
     */
    private static final Kkrt16OriOprfPtoDesc INSTANCE = new Kkrt16OriOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Kkrt16OriOprfPtoDesc() {
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

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KOS15-核COT协议信息。论文来源：
 * <p>
 * Keller M, Orsini E, Scholl P. Actively secure OT extension with optimal overhead. CRYPTO 2015, Springer, Berlin,
 * Heidelberg, 2015, pp. 724-741.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
class Kos15CoreCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3248296565372131498L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KOS15_CORE_COT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
        /**
         * 接收方发送验证信息
         */
        RECEIVER_SEND_CHECK,
        /**
         * 发送方发送随机采样元素
         */
        SENDER_SEND_CHI_POLYNOMIAL,
    }

    /**
     * 单例模式
     */
    private static final Kos15CoreCotPtoDesc INSTANCE = new Kos15CoreCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Kos15CoreCotPtoDesc() {
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

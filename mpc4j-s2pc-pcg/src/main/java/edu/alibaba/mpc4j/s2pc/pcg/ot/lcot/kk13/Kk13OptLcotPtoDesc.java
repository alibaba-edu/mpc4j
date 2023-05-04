package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KK13-2^l选1-COT优化协议信息。论文来源：
 * <p>
 * Kolesnikov V, Kumaresan R. Improved OT Extension for Transferring Short Secrets. CRYPTO 2013, Springer, Berlin,
 * Heidelberg, 2013, pp. 54-70.
 * </p>
 * 本协议使用ALSZ13提出的改进方案降低通信量。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
class Kk13OptLcotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5163500760712773942L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KK13_OPT_LCOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * 单例模式
     */
    private static final Kk13OptLcotPtoDesc INSTANCE = new Kk13OptLcotPtoDesc();

    /**
     * 私有构造函数
     */
    private Kk13OptLcotPtoDesc() {
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

package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KK13-2^l选1-HCOT优化协议信息，使用ALSZ13提出的改进方案降低通信量。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
class Kk13OptLhotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)5163500760712773942L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KK13_OPT_LHOT";

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
    private static final Kk13OptLhotPtoDesc INSTANCE = new Kk13OptLhotPtoDesc();

    /**
     * 私有构造函数
     */
    private Kk13OptLhotPtoDesc() {
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

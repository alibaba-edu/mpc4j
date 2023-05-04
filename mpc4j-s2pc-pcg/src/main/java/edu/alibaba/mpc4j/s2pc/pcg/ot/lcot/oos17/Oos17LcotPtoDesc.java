package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.oos17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OOS17-2^l选1-COT协议信息。论文来源：
 * <p>
 * Orru M, Orsini E, Scholl P. Actively Secure 1-out-of-N OT Extension with Application to Private Set Intersection.
 * CT-RSA 2017, Springer, Cham, 2017: 381-396.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/6/8
 */
class Oos17LcotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 1164697689256738072L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "OOS17_LCOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送随机预言密钥
         */
        SENDER_SEND_RANDOM_ORACLE_KEY,
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
        /**
         * 接收方发送验证信息
         */
        RECEIVER_SEND_CHECK,
    }

    /**
     * 单例模式
     */
    private static final Oos17LcotPtoDesc INSTANCE = new Oos17LcotPtoDesc();

    /**
     * 私有构造函数
     */
    private Oos17LcotPtoDesc() {
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

package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KK13-2^l选1-COT协议信息。论文来源：
 * <p>
 * Kolesnikov V, Kumaresan R. Improved OT Extension for Transferring Short Secrets. CRYPTO 2013, Springer, Berlin,
 * Heidelberg, 2013, pp. 54-70.
 * </p>
 * 本实现兼容Hadamard编码和其他编码方法，从而支持任意更大范围的核2^l选1-OT协议。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
class Kk13OriLcotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)102727658657091283L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KK13_ORI_LOT";

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
    private static final Kk13OriLcotPtoDesc INSTANCE = new Kk13OriLcotPtoDesc();

    /**
     * 私有构造函数
     */
    private Kk13OriLcotPtoDesc() {
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

package edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Beaver91-BC协议信息。此协议基于布尔三元组实现AND运算。论文来源：
 * <p>
 * Beaver, Donald. Efficient multiparty protocols using circuit randomization. CRYPTO 1991, pp. 420-432. Springer,
 * Berlin, Heidelberg, 1991.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
class Bea91BcPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 139609527980746823L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BEA91_BC";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送输入分享值
         */
        SENDER_SEND_INPUT_SHARE,
        /**
         * 接收方发送输入分享值
         */
        RECEIVER_SEND_INPUT_SHARE,
        /**
         * 发送方发送e0和f0
         */
        SENDER_SEND_E0_F0,
        /**
         * 接收方发送e1和f1
         */
        RECEIVER_SEND_E1_F1,
        /**
         * 发送方发送输出分享值
         */
        SENDER_SEND_OUTPUT_SHARE,
        /**
         * 接收方发送输出分享值
         */
        RECEIVER_SEND_SHARE_OUTPUT
    }

    /**
     * 单例模式
     */
    private static final Bea91BcPtoDesc INSTANCE = new Bea91BcPtoDesc();

    /**
     * 私有构造函数
     */
    private Bea91BcPtoDesc() {
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

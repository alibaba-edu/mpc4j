package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LowMc-OPRP协议信息。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
class LowMcOprpPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)2566896299582292732L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "LOWMC_OPRP";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送密钥分享值
         */
        SERVER_SEND_SHARE_KEY,
        /**
         * 客户端发送消息分享值
         */
        CLIENT_SEND_SHARE_MESSAGE,
    }

    /**
     * 单例模式
     */
    private static final LowMcOprpPtoDesc INSTANCE = new LowMcOprpPtoDesc();

    /**
     * 私有构造函数
     */
    private LowMcOprpPtoDesc() {
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

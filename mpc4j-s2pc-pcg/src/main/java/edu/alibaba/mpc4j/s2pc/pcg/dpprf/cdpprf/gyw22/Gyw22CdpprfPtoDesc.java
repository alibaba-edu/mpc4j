package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.gyw22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GYW22-CDPPRF description. The scheme comes from:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class Gyw22CdpprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2106752700961581956L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "YWL20_GF2K_DPPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送纠正比特b
         */
        RECEIVER_SEND_BINARY,
        /**
         * 发送方发送纠正消息M
         */
        SENDER_SEND_MESSAGE,
    }

    /**
     * 单例模式
     */
    private static final Gyw22CdpprfPtoDesc INSTANCE = new Gyw22CdpprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Gyw22CdpprfPtoDesc() {
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

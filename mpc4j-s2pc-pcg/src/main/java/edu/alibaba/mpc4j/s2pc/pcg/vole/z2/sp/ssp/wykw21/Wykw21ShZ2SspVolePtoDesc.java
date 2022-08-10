package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * WYKW21-Z2-SSP-VOLE半诚实安全协议信息。论文来源：
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits." S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
class Wykw21ShZ2SspVolePtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6008340840030209982L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "WYLW21_SH_Z2_SSP_VOLE";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送纠正消息
         */
        RECEIVER_SEND_MESSAGE,
        /**
         * 接收方发送纠正消息D
         */
        RECEIVER_SEND_CORRELATE,
    }

    /**
     * 单例模式
     */
    private static final Wykw21ShZ2SspVolePtoDesc INSTANCE = new Wykw21ShZ2SspVolePtoDesc();

    /**
     * 私有构造函数
     */
    private Wykw21ShZ2SspVolePtoDesc() {
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

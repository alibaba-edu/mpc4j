package edu.alibaba.mpc4j.s2pc.pjc.pid.bkms20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Facebook的字节椭圆曲线PID协议信息。论文来源：
 * <p>
 * Buddhavarapu, Prasad, Andrew Knox, Payman Mohassel, Shubho Sengupta, Erik Taubeneck, and Vlad Vlaskin. Private
 * Matching for Compute. IACR Cryptol. ePrint Arch. 2020 (2020): 599.
 * </p>
 * 此协议实现的是论文图2所描述的PID协议，并不是流式处理协议。
 *
 * @author Weiran Liu
 * @date 2022/9/13
 */
class Bkms20ByteEccPidPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 285531351276926438L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "BKMS20_BYTE_ECC_PID";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送U_c
         */
        SERVER_SEND_UC,
        /**
         * 客户端发送U_p
         */
        CLIENT_SEND_UP,
        /**
         * 服务端发送V_p
         */
        SERVER_SEND_VP,
        /**
         * 客户端发送V_c
         */
        CLIENT_SEND_VC,
        /**
         * 客户端发送E_c
         */
        CLIENT_SEND_EC,
        /**
         * 服务端发送S_c'
         */
        SERVER_SEND_SCP,
        /**
         * 服务端发送S_p
         */
        SERVER_SEND_SP,
        /**
         * 客户端发送S_p'
         */
        CLIENT_SEND_SPP,
    }

    /**
     * 单例模式
     */
    private static final Bkms20ByteEccPidPtoDesc INSTANCE = new Bkms20ByteEccPidPtoDesc();

    /**
     * 私有构造函数
     */
    private Bkms20ByteEccPidPtoDesc() {
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

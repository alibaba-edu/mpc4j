package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZCL22-SKE-PSU协议信息。论文来源：
 * Cong Zhang, Yu Chen, Weiran Liu, Min Zhang, Dongdai Lin. Optimal Private Set Union from Multi-Query Reverse Private
 * Membership Test. manuscript.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
class Zcl22SkePsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)9138205311944704383L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ZCL22_SKE_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送OVDM密钥
         */
        SERVER_SEND_OVDM_KEYS,
        /**
         * 客户端发送OVDM
         */
        CLIENT_SEND_OVDM,
        /**
         * 服务端发送选择比特分享值
         */
        SERVER_SEND_PEQT_SHARES,
        /**
         * 服务端发送加密元素
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * 单例模式
     */
    private static final Zcl22SkePsuPtoDesc INSTANCE = new Zcl22SkePsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Zcl22SkePsuPtoDesc() {
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

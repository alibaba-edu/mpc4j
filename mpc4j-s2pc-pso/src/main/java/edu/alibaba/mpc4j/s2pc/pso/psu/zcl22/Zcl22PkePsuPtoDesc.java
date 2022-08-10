package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZCL22-PKE-PSU协议信息。论文来源：
 * Cong Zhang, Yu Chen, Weiran Liu, Min Zhang, Dongdai Lin. Optimal Private Set Union from Multi-Query Reverse Private
 * Membership Test. manuscript.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
class Zcl22PkePsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)3242597016769861554L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ZCL22_PKE_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送OVDM密钥
         */
        SERVER_SEND_OVDM_KEYS,
        /**
         * 客户端发送公钥
         */
        CLIENT_SEND_PK,
        /**
         * 客户端发送OVDM密文
         */
        CLIENT_SEND_OVDM_KEM,
        /**
         * 客户端发送OVDM负载
         */
        CLIENT_SEND_OVDM_CT,
        /**
         * 客户端发送重随机化密文
         */
        SERVER_SEND_RERAND_KEM,
        /**
         * 客户端发送重随机化负载
         */
        SERVER_SEND_RERAND_CT,
        /**
         * 服务端发送加密元素
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * 单例模式
     */
    private static final Zcl22PkePsuPtoDesc INSTANCE = new Zcl22PkePsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Zcl22PkePsuPtoDesc() {
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

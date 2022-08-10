package edu.alibaba.mpc4j.s2pc.pso.psu.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-PSU协议信息。论文来源：
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
class Gmr21PsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)6323461583044909223L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送密钥
         */
        SERVER_SEND_KEYS,
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * 服务端发送a'
         */
        SERVER_SEND_A_PRIME_OPRFS,
        /**
         * 服务端发送加密元素
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * 单例模式
     */
    private static final Gmr21PsuPtoDesc INSTANCE = new Gmr21PsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21PsuPtoDesc() {
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

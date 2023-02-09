package edu.alibaba.mpc4j.s2pc.pjc.pid.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21宽松PID协议信息。方案来自于下述论文图15（Private-ID protocol）：
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/5/11
 */
class Gmr21SloppyPidPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)8824775557006415344L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "GMR21_SLOPPY_PID";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送密钥
         */
        SERVER_SEND_KEYS,
        /**
         * 客户端发送密钥
         */
        CLIENT_SEND_KEYS,
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 服务端发送OKVS
         */
        SERVER_SEND_OKVS,
        /**
         * 客户端发送并集
         */
        CLIENT_SEND_UNION,
    }

    /**
     * 单例模式
     */
    private static final Gmr21SloppyPidPtoDesc INSTANCE = new Gmr21SloppyPidPtoDesc();

    /**
     * 私有构造函数
     */
    private Gmr21SloppyPidPtoDesc() {
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

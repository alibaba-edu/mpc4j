package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZCL22宽松PMID协议信息。
 *
 * @author Weiran Liu
 * @date 2022/5/14
 */
class Zcl22SloppyPmidPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)8050395384748073492L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ZCL22_SLOPPY_PMID";

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
         * 客户端发送PID的OKVS
         */
        CLIENT_SEND_PID_OKVS,
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 服务端发送PID的OKVS
         */
        SERVER_SEND_PID_OKVS,
        /**
         * 客户端发送clientU的OKVS
         */
        CLIENT_SEND_SIGMA_OKVS,
        /**
         * 服务端发送serverU的OKVS
         */
        SERVER_SEND_SIGMA_OKVS,
        /**
         * 服务端发送PSU集合大小
         */
        SERVER_SEND_PSU_SET_SIZE,
        /**
         * 客户端发送PSU集合大小
         */
        CLIENT_SEND_PSU_SET_SIZE,
        /**
         * 客户端发送并集
         */
        CLIENT_SEND_UNION,
    }

    /**
     * 单例模式
     */
    private static final Zcl22SloppyPmidPtoDesc INSTANCE = new Zcl22SloppyPmidPtoDesc();

    /**
     * 私有构造函数
     */
    private Zcl22SloppyPmidPtoDesc() {
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

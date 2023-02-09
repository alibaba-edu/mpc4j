package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * ZCL22多点PMID协议信息。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
class Zcl22MpPmidPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5496712802788131005L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ZCL22_MP_PMID";

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
         * 客户端发送OKVS
         */
        CLIENT_SEND_SIGMA_OKVS,
        /**
         * 服务端发送OKVS
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
    private static final Zcl22MpPmidPtoDesc INSTANCE = new Zcl22MpPmidPtoDesc();

    /**
     * 私有构造函数
     */
    private Zcl22MpPmidPtoDesc() {
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

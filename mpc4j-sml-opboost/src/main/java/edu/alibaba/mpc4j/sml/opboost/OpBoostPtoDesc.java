package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OpBoost协议信息。
 *
 * @author Weiran Liu
 * @date 2021/10/07
 */
public class OpBoostPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6795257683382517220L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "OP_BOOST";

    /**
     * 协议步骤
     */
    public enum PtoStep {
        /**
         * 从机发送数据格式
         */
        SLAVE_SEND_SCHEMA,
        /**
         * 从机发送排序数据
         */
        SLAVE_SEND_ORDER_DATA_FRAME,
        /**
         * 主机发送排序分割点
         */
        HOST_SEND_ORDER_SPLIT_NODE,
        /**
         * 从机发送分割点
         */
        SLAVE_SEND_SPLIT_NODE,
    }

    /**
     * 单例模式
     */
    private static final OpBoostPtoDesc INSTANCE = new OpBoostPtoDesc();

    /**
     * 私有构造函数
     */
    private OpBoostPtoDesc() {
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

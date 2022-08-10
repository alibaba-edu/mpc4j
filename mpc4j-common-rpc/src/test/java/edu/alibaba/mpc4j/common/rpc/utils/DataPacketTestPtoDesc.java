package edu.alibaba.mpc4j.common.rpc.utils;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 数据包测试协议信息。
 *
 * @author Weiran Liu
 * @date 2022/5/15
 */
class DataPacketTestPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7155287330526658939L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "DATA_PACKET_TEST";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 空数据包
         */
        EMPTY,
        /**
         * 整数数据包
         */
        INTEGER,
        /**
         * 浮点数数据包
         */
        DOUBLE,
        /**
         * 大整数数据包
         */
        BIGINTEGER,
        /**
         * 字节数组数据包
         */
        BYTE_ARRAY,
    }

    /**
     * 单例模式
     */
    private static final DataPacketTestPtoDesc INSTANCE = new DataPacketTestPtoDesc();

    private DataPacketTestPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(DataPacketTestPtoDesc.getInstance());
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

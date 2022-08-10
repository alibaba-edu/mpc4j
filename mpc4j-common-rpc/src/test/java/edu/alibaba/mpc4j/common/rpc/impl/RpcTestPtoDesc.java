package edu.alibaba.mpc4j.common.rpc.impl;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RPC测试协议信息。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class RpcTestPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)1723200316341236086L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RPC_TEST";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 空数据包
         */
        EMPTY,
        /**
         * 长度为0的数据包
         */
        ZERO_LENGTH,
        /**
         * 单条数据
         */
        SINGLE,
        /**
         * 带额外信息
         */
        EXTRA_INFO,
    }

    /**
     * 单例模式
     */
    private static final RpcTestPtoDesc INSTANCE = new RpcTestPtoDesc();

    private RpcTestPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(RpcTestPtoDesc.getInstance());
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

package edu.alibaba.mpc4j.common.rpc.impl;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RPC测试协议信息。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class RpcImplTestPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 1723200316341236086L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RPC_TEST";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * empty
         */
        EMPTY,
        /**
         * length = 0
         */
        ZERO_LENGTH,
        /**
         * singleton
         */
        SINGLETON,
        /**
         * l1
         */
        L1,
        /**
         * l2
         */
        L2,
        /**
         * l4
         */
        L4,
        /**
         * l8
         */
        L8,
        /**
         * equal-length
         */
        EQUAL_LENGTH,
        /**
         * extra information
         */
        EXTRA_INFO,
        /**
         * take-any
         */
        TAKE_ANY,
    }

    /**
     * 单例模式
     */
    private static final RpcImplTestPtoDesc INSTANCE = new RpcImplTestPtoDesc();

    private RpcImplTestPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(RpcImplTestPtoDesc.getInstance());
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

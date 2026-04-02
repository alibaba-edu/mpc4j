package edu.alibaba.mpc4j.common.rpc.impl.netty.robust;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Robust Netty连接协议信息。
 *
 * @author Weiran Liu
 * @date 2026/04/02
 */
class RobustNettyPtoDesc implements PtoDesc {
    /**
     * 协议ID，与SimpleNettyPtoDesc使用不同的值避免命名空间冲突
     */
    private static final int PTO_ID = Math.abs((int) 7219463850124837291L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ROBUST_NETTY_CONNECT";

    /**
     * 协议步骤，与SimpleNettyPtoDesc保持相同结构
     */
    enum StepEnum {
        /**
         * 客户端连接
         */
        CLIENT_CONNECT,
        /**
         * 服务端连接
         */
        SERVER_CONNECT,
        /**
         * 客户端确认
         */
        CLIENT_CONFIRM,
        /**
         * 客户端同步
         */
        CLIENT_SYNCHRONIZE,
        /**
         * 服务端同步
         */
        SERVER_SYNCHRONIZE,
        /**
         * 客户端断开连接
         */
        CLIENT_FINISH,
        /**
         * 服务端断开连接
         */
        SERVER_FINISH,
    }

    /**
     * 单例模式
     */
    private static final RobustNettyPtoDesc INSTANCE = new RobustNettyPtoDesc();

    /**
     * 私有构造函数
     */
    private RobustNettyPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(INSTANCE);
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

package edu.alibaba.mpc4j.common.rpc.impl.netty;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Netty连接协议信息。
 *
 * @author Weiran Liu
 * @date 2021/06/06
 */
class NettyPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)3448038492420117282L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NETTY_CONNECT";

    /**
     * 协议步骤
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
         * 客户端同步
         */
        CLIENT_SYNCHRONIZE,
        /**
         * 服务端同步
         */
        SERVER_SYNCHRONIZE,
        /**
         * 客户端断开链接
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
    private static final NettyPtoDesc INSTANCE = new NettyPtoDesc();

    /**
     * 私有构造函数
     */
    private NettyPtoDesc() {
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

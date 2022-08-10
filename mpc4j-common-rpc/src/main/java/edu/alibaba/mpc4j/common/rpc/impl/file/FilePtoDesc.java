package edu.alibaba.mpc4j.common.rpc.impl.file;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 文件连接协议信息。
 *
 * @author Weiran Liu
 * @date 2022/5/7
 */
class FilePtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)1584853060910634334L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "FILE_CONNECT";

    /**
     * 协议步骤
     */
    enum StepEnum {
        /**
         * 客户端同步
         */
        CLIENT_SYNCHRONIZE,
        /**
         * 服务端同步
         */
        SERVER_SYNCHRONIZE,
    }

    /**
     * 单例模式
     */
    private static final FilePtoDesc INSTANCE = new FilePtoDesc();

    /**
     * 私有构造函数
     */
    private FilePtoDesc() {
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

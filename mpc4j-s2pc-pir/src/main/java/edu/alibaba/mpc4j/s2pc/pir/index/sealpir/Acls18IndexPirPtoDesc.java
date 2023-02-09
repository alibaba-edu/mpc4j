package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * SEAL PIR协议信息。论文来源：
 * <p>
 * Sebastian Angel, Hao Chen, Kim Laine, and Srinath Setty.
 * PIR with compressed queries and amortized query processing.
 * In 2018 IEEE Symposium on Security and Privacy. 2018, 962–979
 * </p>
 * 参考 https://github.com/microsoft/SealPIR 实现。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2505605775962582927L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "SEAL_PIR";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送加密查询
         */
        CLIENT_SEND_QUERY,
        /**
         * 服务端回复密文
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * 单例模式
     */
    private static final Acls18IndexPirPtoDesc INSTANCE = new Acls18IndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Acls18IndexPirPtoDesc() {
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

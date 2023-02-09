package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OnionPIR协议信息。论文来源：
 * <p>
 * Muhammad Haris Mughees, Hao Chen, and Ling Ren. XPIR : OnionPIR: Response Efficient Single-Server PIR.
 * 2021 ACM SIGSAC Conference on Computer and Communications Security. 2021, 15–19
 * </p>
 * 原始方案使用SEAL和NFLlib实现，这里应用SEAL实现。
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class Mcr21IndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 1557128141245400138L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ONION_PIR";

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
    private static final Mcr21IndexPirPtoDesc INSTANCE = new Mcr21IndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Mcr21IndexPirPtoDesc() {
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

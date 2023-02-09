package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * FastPIR协议信息。论文来源：
 * <p>
 * Ishtiyaque Ahmad, Yuntian Yang, Divyakant Agrawal, Amr El Abbadi, and Trinabh Gupta.
 * Addra: Metadata-private voice communication over fully untrusted infrastructure.
 * In 15th USENIX Symposium on Operating Systems Design and Implementation, OSDI. 2021, 313-329
 * </p>
 * 参考 https://github.com/ishtiyaque/FastPIR 实现。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 4569274851469795906L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "FAST_PIR";

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
    private static final Ayaa21IndexPirPtoDesc INSTANCE = new Ayaa21IndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Ayaa21IndexPirPtoDesc() {
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

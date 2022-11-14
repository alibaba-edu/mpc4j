package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * XPIR协议信息。论文来源：
 * <p>
 * Carlos Aguilar Melchor, Joris Barrier, Laurent Fousse, and Marc-Olivier Killijian. XPIR : Private Information
 * Retrieval for Everyone. Proc. Priv. Enhancing Technol. 2016, 2 (2016), 155–174
 * </p>
 * 原始方案使用libNTL实现，这里应用SEAL实现。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 5618466453562454763L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MBFK16_INDEX_PIR";

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
    private static final Mbfk16IndexPirPtoDesc INSTANCE = new Mbfk16IndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Mbfk16IndexPirPtoDesc() {
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

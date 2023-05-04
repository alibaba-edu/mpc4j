package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Vectorized PIR协议信息。论文来源：
 * <p>
 * Muhammad Haris Mughees and Ling Ren. Vectorized Batch Private Information Retrieval.
 * To appear in 44th IEEE Symposium on Security and Privacy, 2023.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6504767861642733857L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "VectorizedPIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * 单例模式
     */
    private static final Mr23IndexPirPtoDesc INSTANCE = new Mr23IndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr23IndexPirPtoDesc() {
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

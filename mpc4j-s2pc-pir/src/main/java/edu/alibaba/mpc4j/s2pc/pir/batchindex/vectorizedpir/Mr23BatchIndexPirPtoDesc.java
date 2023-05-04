package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * VECTORIZED_BATCH_PIR协议信息。论文来源：
 * <p>
 * Muhammad Haris Mughees and Ling Ren. Vectorized Batch Private Information Retrieval.
 * To appear in 44th IEEE Symposium on Security and Privacy, 2023.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6854774536447892257L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "VECTORIZED_BATCH_PIR";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送加密查询
         */
        CLIENT_SEND_QUERY,
        /**
         * 服务端回复密文
         */
        SERVER_SEND_RESPONSE,
        /**
         * 客户端发送公钥
         */
        CLIENT_SEND_PUBLIC_KEYS,
    }

    /**
     * 单例模式
     */
    private static final Mr23BatchIndexPirPtoDesc INSTANCE = new Mr23BatchIndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr23BatchIndexPirPtoDesc() {
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

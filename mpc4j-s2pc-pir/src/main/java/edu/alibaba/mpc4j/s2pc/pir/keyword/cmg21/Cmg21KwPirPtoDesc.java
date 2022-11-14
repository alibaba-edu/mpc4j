package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CMG21关键词索引PIR协议信息。论文来源：
 * <p>
 * Cong, Kelong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael
 * Rosenberg. Labeled psi from homomorphic encryption with reduced computation and communication. ACM CCS 2021, pp.
 * 1135-1150. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7261080771728862744L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CMG21_KEYWORD_PIR";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送布谷鸟哈希密钥
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送布谷鸟哈希是否分桶成功
         */
        CLIENT_SEND_CUCKOO_HASH_RESULT,
        /**
         * 客户端发送加密方案密钥
         */
        CLIENT_SEND_FHE_PARAMS,
        /**
         * 客户端发送加密查询
         */
        CLIENT_SEND_QUERY,
        /**
         * 服务端返回元素密文
         */
        SERVER_SEND_ITEM_RESPONSE,
        /**
         * 服务端返回标签密文
         */
        SERVER_SEND_LABEL_RESPONSE,
        /**
         * 客户端发送盲化元素
         */
        CLIENT_SEND_BLIND,
        /**
         * 服务端返回客户端盲化元素PRF
         */
        SERVER_SEND_BLIND_PRF,
    }

    /**
     * 单例模式
     */
    private static final Cmg21KwPirPtoDesc INSTANCE = new Cmg21KwPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Cmg21KwPirPtoDesc() {
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

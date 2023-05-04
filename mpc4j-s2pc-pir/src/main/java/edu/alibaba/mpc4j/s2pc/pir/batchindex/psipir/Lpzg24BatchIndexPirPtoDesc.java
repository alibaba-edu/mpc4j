package edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSI-PIR协议信息。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzg24BatchIndexPirPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3563364173424535189L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PSI_PIR";

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
         * 客户端发送盲化元素
         */
        CLIENT_SEND_BLIND,
        /**
         * 服务端返回客户端盲化元素PRF
         */
        SERVER_SEND_BLIND_PRF,
        /**
         * 客户端发送公钥
         */
        CLIENT_SEND_PUBLIC_KEYS,
    }

    /**
     * 单例模式
     */
    private static final Lpzg24BatchIndexPirPtoDesc INSTANCE = new Lpzg24BatchIndexPirPtoDesc();

    /**
     * 私有构造函数
     */
    private Lpzg24BatchIndexPirPtoDesc() {
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

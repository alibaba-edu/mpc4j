package edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CMG21非平衡PSI协议信息。论文来源：
 * <p>
 * Cong, Kelong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael
 * Rosenberg. Labeled psi from homomorphic encryption with reduced computation and communication. ACM CCS 2021, pp.
 * 1135-1150. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class Cmg21UpsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6265841553375230711L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CMG21_UPSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 客户端发送加密方案密钥
         */
        CLIENT_SEND_ENCRYPTION_PARAMS,
        /**
         * 客户端发送加密查询
         */
        CLIENT_SEND_QUERY,
        /**
         * 服务端返回密文匹配结果
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * 单例模式
     */
    private static final Cmg21UpsiPtoDesc INSTANCE = new Cmg21UpsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Cmg21UpsiPtoDesc() {
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

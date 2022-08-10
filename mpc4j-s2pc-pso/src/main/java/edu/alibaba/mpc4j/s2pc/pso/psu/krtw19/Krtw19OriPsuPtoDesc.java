package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KRTW19-ORI-PSU协议信息。原始论文：
 * Kolesnikov V, Rosulek M, Trieu N, et al. Scalable private set union from symmetric-key techniques. ASIACRYPT 2019,
 * pp. 636-666.
 * 此实现为论文描述的实现。KRTW19开源代码的实现有进一步的优化方法。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
class Krtw19OriPsuPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)5986401728116628943L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KRTW19_ORI_PSU";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送各个密钥参数
         */
        SERVER_SEND_KEYS,
        /**
         * 客户端发送OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * 服务端发送PEQT验证数据
         */
        SERVER_SEND_S_STAR_OPRFS,
        /**
         * 服务端发送加密元素
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * 单例模式
     */
    private static final Krtw19OriPsuPtoDesc INSTANCE = new Krtw19OriPsuPtoDesc();

    /**
     * 私有构造函数
     */
    private Krtw19OriPsuPtoDesc() {
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

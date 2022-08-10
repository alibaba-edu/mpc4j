package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CM20-MPOPRF协议描述。CM20协议最初由下述论文提出：
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * 后续，下述论文对CM20的本质进行了抽象，提出了MP-OPRF的概念：
 * Jia, Yanxue, Shi-Feng Sun, Hong-Sheng Zhou, Jiajun Du, and Dawu Gu. Shuffle-based Private Set Union: Faster and More
 * Secure. USENIX Security 2022.
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
class Cm20MpOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)132060736192853349L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CM20_MPOPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送编码密钥
         */
        RECEIVER_SEND_KEY,
        /**
         * 接收方发送矩阵
         */
        RECEIVER_SEND_DELTA,
    }

    /**
     * 单例模式
     */
    private static final Cm20MpOprfPtoDesc INSTANCE = new Cm20MpOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Cm20MpOprfPtoDesc() {
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

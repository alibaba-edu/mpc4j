package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-UNI-MSP-COT协议信息。此实现是下述论文图7的多点COT实现，使用了布谷鸟哈希，论文来源：
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
class Ywl20UniMspCotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6799413282773110363L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "YWL20_UNI_MSP_COT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送布谷鸟哈希密钥
         */
        RECEIVER_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * 单例模式
     */
    private static final Ywl20UniMspCotPtoDesc INSTANCE = new Ywl20UniMspCotPtoDesc();

    /**
     * 私有构造函数
     */
    private Ywl20UniMspCotPtoDesc() {
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

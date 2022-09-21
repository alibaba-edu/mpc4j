package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * MR19-Kyber-基础n选1-OT协议信息。论文来源：
 * <p>
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * </p>
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/25
 */
public class Mr19KyberBaseNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2227932246488015483L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MR19_KYBER_BASE_NOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送公钥RN
         */
        RECEIVER_SEND_PK,
        /**
         * 发送方发送参数B
         */
        SENDER_SEND_BETA,
    }

    /**
     * 单例模式
     */
    private static final Mr19KyberBaseNotPtoDesc INSTANCE = new Mr19KyberBaseNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr19KyberBaseNotPtoDesc() {
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

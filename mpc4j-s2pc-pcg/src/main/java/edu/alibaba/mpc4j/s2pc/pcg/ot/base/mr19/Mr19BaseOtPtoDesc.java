package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MR19-基础OT协议信息。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * Vladimir Kolesnikov, Ranjit Kumaresan, Mike Rosulek, Ni Trieu. Efficient Batched Oblivious PRF with
 * Applications to Private Set Intersection. CCS 2016: 818-829。
 *
 * @author Weiran Liu
 * @date 2020/10/03
 */
class Mr19BaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)7464266642234682892L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MR19_BASE_OT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送参数R0、R1
         */
        RECEIVER_SEND_R,
        /**
         * 发送方发送参数B
         */
        SENDER_SEND_B,
    }
    /**
     * 单例模式
     */
    private static final Mr19BaseOtPtoDesc INSTANCE = new Mr19BaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr19BaseOtPtoDesc() {
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
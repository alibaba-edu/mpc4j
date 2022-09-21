package edu.alibaba.mpc4j.s2pc.pcg.ot.base.mr19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MR19-椭圆曲线-基础OT协议信息。论文来源：
 * <p>
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * </p>
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * McQuoid I, Rosulek M, Roy L. Batching base oblivious transfers. ASIACRYPT 2021, Springer, Cham, 2021: 281-310.
 * </p>
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/10/03
 */
class Mr19EccBaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7464266642234682892L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MR19_ECC_BASE_OT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送参数R0、R1
         */
        RECEIVER_SEND_R,
        /**
         * 发送方发送参数β
         */
        SENDER_SEND_BETA,
    }

    /**
     * 单例模式
     */
    private static final Mr19EccBaseOtPtoDesc INSTANCE = new Mr19EccBaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr19EccBaseOtPtoDesc() {
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
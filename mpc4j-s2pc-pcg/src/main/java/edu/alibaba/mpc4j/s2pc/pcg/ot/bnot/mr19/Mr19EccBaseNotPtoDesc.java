package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * MR19-椭圆曲线-基础n选1-OT协议信息。论文来源：
 * <p>
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 * </p>
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * Vladimir Kolesnikov, Ranjit Kumaresan, Mike Rosulek, Ni Trieu. Efficient Batched Oblivious PRF with
 * Applications to Private Set Intersection. CCS 2016: 818-829。
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/07/26
 */
class Mr19EccBaseNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 2049550450333885121L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "MR19_ECC_BASE_NOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送参数R0、R1
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
    private static final Mr19EccBaseNotPtoDesc INSTANCE = new Mr19EccBaseNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Mr19EccBaseNotPtoDesc() {
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
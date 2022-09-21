package edu.alibaba.mpc4j.s2pc.pcg.ot.base.co15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CO15-基础OT协议信息。此方案实现论文Introduction提供的2选1-OT。论文来源：
 * <p>
 * Chou T, Orlandi C. The simplest protocol for oblivious transfer. LATINCRYPT 2015, Springer, 2015, pp. 40-58.
 * </p>
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * McQuoid I, Rosulek M, Roy L. Batching base oblivious transfers. ASIACRYPT 2021, Springer, Cham, 2021: 281-310.
 * </p>
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2020/08/13
 */
class Co15BaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6872189019363135094L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CO15_BASE_OT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送参数S
         */
        SENDER_SEND_S,
        /**
         * 接收方发送参数R
         */
        RECEIVER_SEND_R,
    }

    /**
     * 单例模式
     */
    private static final Co15BaseOtPtoDesc INSTANCE = new Co15BaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Co15BaseOtPtoDesc() {
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

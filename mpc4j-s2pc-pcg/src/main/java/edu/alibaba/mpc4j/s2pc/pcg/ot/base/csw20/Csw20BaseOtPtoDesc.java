package edu.alibaba.mpc4j.s2pc.pcg.ot.base.csw20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Csw20-基础OT协议信息。论文来源：
 * <p>
 * Canetti R, Sarkar P, Wang X. Blazing Fast OT for Three-Round UC OT Extension. PKC 2020, Springer, 2020, pp. 299-327.
 * </p>
 * 采用了安全批处理操作，提高批量生成效率。批处理方法来自论文：
 * <p>
 * McQuoid I, Rosulek M, Roy L. Batching base oblivious transfers. ASIACRYPT 2021, Springer, Cham, 2021: 281-310.
 * </p>
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/04/26
 */
class Csw20BaseOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 7218569350942200596L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CSW20_BASE_OT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送参数C
         */
        RECEIVER_SEND_C,
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
    private static final Csw20BaseOtPtoDesc INSTANCE = new Csw20BaseOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Csw20BaseOtPtoDesc() {
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

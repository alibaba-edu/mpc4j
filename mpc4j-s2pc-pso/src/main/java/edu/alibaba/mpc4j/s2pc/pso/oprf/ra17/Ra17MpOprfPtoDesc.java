package edu.alibaba.mpc4j.s2pc.pso.oprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RA17-OPRF协议信息。论文来源：
 * Resende A C D, Aranha D F. Faster unbalanced private set intersection. FC 2018. Springer, Berlin, Heidelberg,
 * pp. 203-221.
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
class Ra17MpOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)4773039327699423585L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RA17_MPOPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送盲化元素
         */
        RECEIVER_SEND_BLIND,
        /**
         * 发送方返回接收方盲化元素PRF
         */
        SENDER_SEND_BLIND_PRF,
    }

    /**
     * 单例模式
     */
    private static final Ra17MpOprfPtoDesc INSTANCE = new Ra17MpOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Ra17MpOprfPtoDesc() {
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

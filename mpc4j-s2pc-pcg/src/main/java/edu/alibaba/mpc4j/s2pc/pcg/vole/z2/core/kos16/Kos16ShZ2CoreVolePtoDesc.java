package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KOS16-Z2-半诚实安全核VOLE协议信息。论文来源：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
class Kos16ShZ2CoreVolePtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)9214585919459665568L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KOS16_SH_Z2_CORE_VOLE";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送矩阵
         */
        SENDER_SEND_MATRIX,
    }

    /**
     * 单例模式
     */
    private static final Kos16ShZ2CoreVolePtoDesc INSTANCE = new Kos16ShZ2CoreVolePtoDesc();

    /**
     * 私有构造函数
     */
    private Kos16ShZ2CoreVolePtoDesc() {
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

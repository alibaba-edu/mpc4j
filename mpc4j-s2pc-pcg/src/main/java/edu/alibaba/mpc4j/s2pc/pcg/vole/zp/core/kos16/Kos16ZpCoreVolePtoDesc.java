package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KOS16-Zp-核VOLE协议信息。论文来源：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public class Kos16ZpCoreVolePtoDesc implements PtoDesc {
    /**
     * 协议ID。
     */
    private static final int PTO_ID = Math.abs((int) 2047864806225283374L);
    /**
     * 协议名称。
     */
    private static final String PTO_NAME = "KOS16_ZP_CORE_VOLE";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送矩阵
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * 单例模式。
     */
    private static final Kos16ZpCoreVolePtoDesc INSTANCE = new Kos16ZpCoreVolePtoDesc();

    /**
     * 私有构造函数。
     */
    private Kos16ZpCoreVolePtoDesc() {
        // empty
    }

    /**
     * 获取静态实例。
     */
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

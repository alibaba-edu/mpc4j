package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RR17 Encode-Commit PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Peter Rindal, Mike Rosulek. Malicious-Secure Private Set Intersection via Dual Execution.
 * ACM CCS 2017
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -3390158486732003692L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RR17_EC_PSI";

    /**
     * protocol steps
     */
    enum PtoStep {
        /**
         * server send PRF of items in hash bins
         */
        SERVER_SEND_TUPLES,
    }

    /**
     * singleton mode
     */
    private static final Rr17EcPsiPtoDesc INSTANCE = new Rr17EcPsiPtoDesc();

    /**
     * private constructor.
     */
    private Rr17EcPsiPtoDesc() {
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

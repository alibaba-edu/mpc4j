package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RR17 Dual Execution PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Peter Rindal, Mike Rosulek. Malicious-Secure Private Set Intersection via Dual Execution.
 * ACM CCS 2017
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -947977268059832451L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RR17_DE_PSI";

    /**
     * protocol steps
     */
    enum PtoStep {
        /**
         * server send PRFs to client
         */
        SERVER_SEND_PRFS,
    }

    /**
     * singleton mode
     */
    private static final Rr17DePsiPtoDesc INSTANCE = new Rr17DePsiPtoDesc();

    /**
     * private constructor.
     */
    private Rr17DePsiPtoDesc() {
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
package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRTY19-PSI (low communication) protocol description. The protocol comes from the following paper:
 * <p>
 * Benny Pinkas, Mike Rosulek, et al. SpOT-Light- Lightweight Private Set Intersection from Sparse OT Extension.
 * CRYPTO 2019, pp. 401–431.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
class Prty19LowPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 962279898847567317L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PRTY19_LOW_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends OKVS key
         */
        CLIENT_SEND_OKVS_KEY,
        /**
         * client sends OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * server sends PRFs.
         */
        SERVER_SEND_PRFS,
    }
    /**
     * singleton mode
     */
    private static final Prty19LowPsiPtoDesc INSTANCE = new Prty19LowPsiPtoDesc();

    /**
     * private constructor.
     */
    private Prty19LowPsiPtoDesc() {
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

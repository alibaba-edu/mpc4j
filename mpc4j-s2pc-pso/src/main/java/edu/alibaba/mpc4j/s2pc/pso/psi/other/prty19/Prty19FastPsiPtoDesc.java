package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRTY19-PSI (fast computation) protocol description. The protocol comes from the following paper:
 * <p>
 * Benny Pinkas, Mike Rosulek, et al. SpOT-Light- Lightweight Private Set Intersection from Sparse OT Extension.
 * CRYPTO 2019, pp. 401–431.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
class Prty19FastPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5838309125365213426L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PRTY19_FAST_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends two-choice hash key
         */
        CLIENT_SEND_TWO_CHOICE_HASH_KEY,
        /**
         * client sends OKVS array
         */
        CLIENT_SEND_POLYNOMIALS,
        /**
         * server sends the first PRFs
         */
        SERVER_SEND_PRFS_0,
        /**
         * server sends the second PRFs
         */
        SERVER_SEND_PRFS_1,
    }

    /**
     * singleton mode
     */
    private static final Prty19FastPsiPtoDesc INSTANCE = new Prty19FastPsiPtoDesc();

    /**
     * private constructor.
     */
    private Prty19FastPsiPtoDesc() {
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

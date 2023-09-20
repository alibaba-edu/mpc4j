package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KRTW19-PSU protocol description. The scheme is described in the following paper:
 * <p>
 * Kolesnikov V, Rosulek M, Trieu N, et al. Scalable private set union from symmetric-key techniques. ASIACRYPT 2019,
 * pp. 636-666.
 * </p>
 * The implementation follows the open-source code in the paper, i.e., root polynomial instead of interpolation.
 *
 * @author Weiran Liu
 * @date 2022/02/20
 */
class Krtw19PsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int)1647093902110062076L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KRTW19_PSU";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends keys
         */
        SERVER_SEND_KEYS,
        /**
         * client sends polynomials
         */
        CLIENT_SEND_POLYS,
        /**
         * server sends S* OPRFs.
         */
        SERVER_SEND_S_STAR_OPRFS,
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final Krtw19PsuPtoDesc INSTANCE = new Krtw19PsuPtoDesc();

    /**
     * private constructor.
     */
    private Krtw19PsuPtoDesc() {
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

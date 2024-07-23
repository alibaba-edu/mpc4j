package edu.alibaba.mpc4j.s2pc.pso.psu.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GMR21-PSU protocol decryption. The protocol comes from the following paper:
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
class Gmr21PsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6323461583044909223L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_PSU";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final Gmr21PsuPtoDesc INSTANCE = new Gmr21PsuPtoDesc();

    /**
     * private constructor
     */
    private Gmr21PsuPtoDesc() {
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

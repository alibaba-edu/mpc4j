package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * semi-honest YWL20-BSP-COT protocol description. The construction comes from the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class Ywl20ShBspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6366716798346786692L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_SH_BSP_COT";
    /**
     * singleton mode
     */
    private static final Ywl20ShBspCotPtoDesc INSTANCE = new Ywl20ShBspCotPtoDesc();

    /**
     * private constructor.
     */
    private Ywl20ShBspCotPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends C
         */
        SENDER_SEND_CORRELATE,
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

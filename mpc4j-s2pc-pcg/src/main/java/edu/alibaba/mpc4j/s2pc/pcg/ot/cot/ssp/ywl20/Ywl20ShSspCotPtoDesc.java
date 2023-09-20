package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * semi-honest YWL20-SSP-COT protocol description. The construction comes from the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
class Ywl20ShSspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1703312121844029795L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_SH_SSP_COT";
    /**
     * singleton mode
     */
    private static final Ywl20ShSspCotPtoDesc INSTANCE = new Ywl20ShSspCotPtoDesc();

    /**
     * private constructor.
     */
    private Ywl20ShSspCotPtoDesc() {
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

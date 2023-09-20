package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * malicious YWL20-SSP-COT protocol description. The construction comes from the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/19
 */
class Ywl20MaSspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6565040122362017744L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_MA_SSP_COT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends the random oracle key
         */
        RECEIVER_SEND_RANDOM_ORACLE_KEY,
        /**
         * sender sends C
         */
        SENDER_SEND_CORRELATE,
        /**
         * receiver sends x'
         */
        RECEIVER_SEND_CHECK_CHOICES,
        /**
         * sender sends H'(V)
         */
        SENDER_SEND_HASH_VALUE,
    }

    /**
     * singleton mode
     */
    private static final Ywl20MaSspCotPtoDesc INSTANCE = new Ywl20MaSspCotPtoDesc();

    /**
     * private constructor
     */
    private Ywl20MaSspCotPtoDesc() {
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

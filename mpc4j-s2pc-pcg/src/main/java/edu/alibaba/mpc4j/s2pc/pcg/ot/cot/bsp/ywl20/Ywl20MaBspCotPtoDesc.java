package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * malicious YWL20-BSP-COT protocol description. The protocol comes from the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * We leverage the batched consistency check shown in Appendix B: Batched Consistency Check.
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
class Ywl20MaBspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5636166080693023093L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_MA_BSP_COT";

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
    private static final Ywl20MaBspCotPtoDesc INSTANCE = new Ywl20MaBspCotPtoDesc();

    /**
     * private constructor
     */
    private Ywl20MaBspCotPtoDesc() {
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

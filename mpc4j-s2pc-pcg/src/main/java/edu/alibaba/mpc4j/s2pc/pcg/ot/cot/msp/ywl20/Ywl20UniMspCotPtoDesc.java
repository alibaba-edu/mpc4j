package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * YWL20-UNI-MSP-COT protocol description. The protocol is described in the following paper, Figure 7:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
class Ywl20UniMspCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6799413282773110363L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_UNI_MSP_COT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends cuckoo hash keys
         */
        RECEIVER_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * singleton mode
     */
    private static final Ywl20UniMspCotPtoDesc INSTANCE = new Ywl20UniMspCotPtoDesc();

    /**
     * private constructor.
     */
    private Ywl20UniMspCotPtoDesc() {
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

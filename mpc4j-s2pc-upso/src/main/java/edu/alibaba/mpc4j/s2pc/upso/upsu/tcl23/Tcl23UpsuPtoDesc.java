package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * TCL23 UPSU protocol description.
 * The protocol comes from the construction of the following paper:
 * <p>
 * Binbin Tu, Yu Chen, Qi Liu, and Cong Zhang.
 * Fast Unbalanced Private Set Union from Fully Homomorphic Encryption. CCS 2023, pp. 2959-2973.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class Tcl23UpsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4437869771412085476L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TCL23_UPSU";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * receiver sends cuckoo hash keys
         */
        RECEIVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * sender sends encryption params
         */
        SENDER_SEND_PUBLIC_KEYS,
        /**
         * sender sends query
         */
        SENDER_SEND_QUERY,
        /**
         * receiver sends response
         */
        RECEIVER_SEND_RESPONSE,
        /**
         * sender sends encrypted elements
         */
        SENDER_SEND_ENC_ELEMENTS
    }

    /**
     * the singleton mode
     */
    private static final Tcl23UpsuPtoDesc INSTANCE = new Tcl23UpsuPtoDesc();

    /**
     * private constructor.
     */
    private Tcl23UpsuPtoDesc() {
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

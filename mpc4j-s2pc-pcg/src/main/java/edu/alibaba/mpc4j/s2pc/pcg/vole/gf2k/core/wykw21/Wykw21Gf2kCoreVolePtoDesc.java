package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * WYKW21-GF2K-core VOLE description. The protocol comes from:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 * The original scheme invokes semi-honest GF2K-core VOLE (i.e., COPEe). Here we void possibly recursive invoke and
 * directly implement this by using base OT.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21Gf2kCoreVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4870725867402299345L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "WYKW21_GF2K_CORE_VOLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends the random oracle key
         */
        RECEIVER_SEND_RANDOM_ORACLE_KEY,
        /**
         * receiver sends the matrix
         */
        RECEIVER_SEND_MATRIX,
        /**
         * receiver sends challenge χ
         */
        RECEIVER_SEND_CHALLENGE_CHI,
        /**
         * sender sends response χ
         */
        SENDER_SEND_RESPONSE_CHI,
    }

    /**
     * singleton mode
     */
    private static final Wykw21Gf2kCoreVolePtoDesc INSTANCE = new Wykw21Gf2kCoreVolePtoDesc();

    /**
     * private constructor
     */
    private Wykw21Gf2kCoreVolePtoDesc() {
        // empty
    }

    /**
     * Gets the protocol description.
     *
     * @return the protocol description.
     */
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

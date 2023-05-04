package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.crypto.phe.PheSecLevel;

/**
 * DSZ15 Zl HE-based core multiplication triple generation protocol description. The original scheme is described in
 * Section 3.A.4:
 * <p>
 * Daniel Demmler, Thomas Schneider, Michael Zohner: ABY - A Framework for Efficient Mixed-Protocol Secure Two-Party
 * Computation. NDSS 2015.
 * </p>
 * Here we leverage the packing technique. See the following two papers for details:
 * <p>
 * Pullonen, Pille, Dan Bogdanov, and Thomas Schneider. The design and implementation of a two-party protocol suite for
 * Sharemind 3. CYBERNETICA Institute of Information Security, Tech. Rep 4 (2012): 17.
 * </p>
 * <p>
 * Pullonen, Pille. Actively secure two-party computation: Efficient beaver triple generation. Instructor (2013).
 * </p>
 *
 * @author Li Peng, Weiran Liu
 * @date 2023/2/20
 */
public class Dsz15HeZlCoreMtgPtoDesc implements PtoDesc {
    /**
     * the default PHE security level
     */
    static final PheSecLevel PHE_SEC_LEVEL = PheSecLevel.LAMBDA_128;
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8254761070183800831L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "DSZ15_HE_Zl_CORE_MTG";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends the PHE public key
         */
        SENDER_SEND_PHE_PUBLIC_KEY,
        /**
         * the sender sends the ciphertext
         */
        SENDER_SEND_CIPHERTEXT,
        /**
         * the receiver sends the operated ciphertext
         */
        RECEIVER_SEND_CIPHERTEXT,
    }

    /**
     * the singleton mode
     */
    private static final Dsz15HeZlCoreMtgPtoDesc INSTANCE = new Dsz15HeZlCoreMtgPtoDesc();

    /**
     * the private constructor
     */
    private Dsz15HeZlCoreMtgPtoDesc() {
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

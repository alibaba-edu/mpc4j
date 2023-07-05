package edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LowMc single-query OPRF protocol description. This protocol is implicitly introduced in the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Nigel P. Smart, and Stephen C. Williams. Secure two-party computation is practical.
 * ASIACRYPT 2009, Proceedings 15, pp. 250-267. Springer Berlin Heidelberg, 2009.
 * <p/>
 * Here, we use LowMc instead of AES with the goal of reducing the number of AND gates. Specifically, the implementation
 * here refers to Figure 4 in the following paper:
 * <p>
 * Kiss, Ágnes, Jian Liu, Thomas Schneider, N. Asokan, and Benny Pinkas. Private Set Intersection for Unequal Set Sizes
 * with Mobile Applications. PETS 2017, no.4, pp. 97-117.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
class Pssw09SqOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8968002409527651469L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PSSW09_SQ_OPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends shares of OPRF result
         */
        SENDER_SEND_SHARES,
    }

    /**
     * singleton mode
     */
    private static final Pssw09SqOprfPtoDesc INSTANCE = new Pssw09SqOprfPtoDesc();

    /**
     * private constructor
     */
    private Pssw09SqOprfPtoDesc() {
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

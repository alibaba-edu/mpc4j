package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * NR04-based single-query OPRF. This protocol is implicitly introduced in the following paper:
 * <p>
 * Naor, Moni, and Omer Reingold. Number-theoretic constructions of efficient pseudo-random functions. Journal of the
 * ACM (JACM) 51, no. 2 (2004): 231-262.
 * </p>
 * The implementation here refers to Figure 3 in the following paper:
 * <p>
 * Kiss, Ágnes, Jian Liu, Thomas Schneider, N. Asokan, and Benny Pinkas. Private Set Intersection for Unequal Set Sizes
 * with Mobile Applications. PETS 2017, no.4, pp. 97-117.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
class Nr04EccSqOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4711347230656983538L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "NR04_ECC_SQ_OPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * Sender sends g^{r^{-1}}
         */
        SENDER_SEND_GR_INV,
        /**
         * Sender sends masked R0 and R1
         */
        SENDER_SEND_MESSAGE,
    }

    /**
     * singleton mode
     */
    private static final Nr04EccSqOprfPtoDesc INSTANCE = new Nr04EccSqOprfPtoDesc();

    /**
     * private constructor.
     */
    private Nr04EccSqOprfPtoDesc() {
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

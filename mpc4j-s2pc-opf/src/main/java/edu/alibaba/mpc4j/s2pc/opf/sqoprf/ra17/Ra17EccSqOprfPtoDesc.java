package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RA17 ECC single-query OPRF protocol description. This protocol is implicitly introduced in the following paper:
 * <p>
 * Resende, Amanda C. Davi, and Diego F. Aranha. Faster unbalanced private set intersection. In FC 2018, pp. 203-221.
 * Springer Berlin Heidelberg, 2018.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
class Ra17EccSqOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4805456138827269998L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RA17_ECC_SQ_OPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the receiver sends the blind
         */
        RECEIVER_SEND_BLIND,
        /**
         * the sender sends the blind prf
         */
        SENDER_SEND_BLIND_PRF,
    }

    /**
     * singleton mode
     */
    private static final Ra17EccSqOprfPtoDesc INSTANCE = new Ra17EccSqOprfPtoDesc();

    /**
     * private constructor
     */
    private Ra17EccSqOprfPtoDesc() {
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

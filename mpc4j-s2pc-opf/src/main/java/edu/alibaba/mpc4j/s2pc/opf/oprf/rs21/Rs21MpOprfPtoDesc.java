package edu.alibaba.mpc4j.s2pc.opf.oprf.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RS21-MP-OPRF protocol description. This OPRF is described in the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021, pp.
 * 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
class Rs21MpOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7835047985558629286L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RS21-MP-OPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends c^s
         */
        SENDER_SEND_CS,
        /**
         * receiver sends OKVS, including key (r) and masked OKVS storage (P + A'), with w^r
         */
        RECEIVER_SEND_OKVS_WR,
        /**
         * sender sends w^s
         */
        SENDER_SEND_WS,
    }

    /**
     * singleton mode
     */
    private static final Rs21MpOprfPtoDesc INSTANCE = new Rs21MpOprfPtoDesc();

    /**
     * private constructor.
     */
    private Rs21MpOprfPtoDesc() {
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

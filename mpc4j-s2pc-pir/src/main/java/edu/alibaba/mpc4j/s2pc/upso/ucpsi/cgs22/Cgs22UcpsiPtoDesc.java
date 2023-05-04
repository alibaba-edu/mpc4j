package edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 unbalanced circuit PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient circuit-based PSI with linear
 * communication. EUROCRYPT 2019, Part III, pp. 122-153. Springer International Publishing, 2019.
 * </p>
 * The implementation has linear communication with stash-less cuckoo hashing.
 *
 * @author Weiran Liu
 * @date 2023/4/20
 */
class Cgs22UcpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 482602283261307239L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "CGS22-UCPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends cuckoo hash keys
         */
        SERVER_SEND_HASH_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final Cgs22UcpsiPtoDesc INSTANCE = new Cgs22UcpsiPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22UcpsiPtoDesc() {
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

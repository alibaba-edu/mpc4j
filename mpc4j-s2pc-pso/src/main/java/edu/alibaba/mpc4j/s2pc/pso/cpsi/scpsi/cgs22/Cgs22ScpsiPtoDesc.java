package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 server-payload circuit PSI protocol description. This protocol comes from the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
class Cgs22ScpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1658610450267699356L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "CGS22-SCPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the server sends cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final Cgs22ScpsiPtoDesc INSTANCE = new Cgs22ScpsiPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22ScpsiPtoDesc() {
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

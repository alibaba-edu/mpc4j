package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 client-payload circuit PSI protocol description. This protocol comes from the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/20
 */
class Cgs22CcpsiPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 638658871633103900L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "CGS22-CCPSI";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the client sends cuckoo hash keys
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
    }

    /**
     * the singleton mode
     */
    private static final Cgs22CcpsiPtoDesc INSTANCE = new Cgs22CcpsiPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22CcpsiPtoDesc() {
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

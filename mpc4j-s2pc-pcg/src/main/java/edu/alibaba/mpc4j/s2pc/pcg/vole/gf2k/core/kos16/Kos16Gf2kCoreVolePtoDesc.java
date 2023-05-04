package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KOS16-GF2K-core VOLE description. The protocol comes from:
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Kos16Gf2kCoreVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4846966591315685521L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KOS16_GF2K_CORE_VOLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends the matrix
         */
        RECEIVER_SEND_MATRIX,
    }

    /**
     * singleton mode
     */
    private static final Kos16Gf2kCoreVolePtoDesc INSTANCE = new Kos16Gf2kCoreVolePtoDesc();

    /**
     * private constructor
     */
    private Kos16Gf2kCoreVolePtoDesc() {
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

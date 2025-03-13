package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of three-party lowmc soprp protocols.
 * The scheme comes from the following paper:
 *
 * <p>
 * Martin Albrecht, Christian Rechberger, et al.
 * Ciphers for MPC and FHE.
 * EUROCRYPT 2015
 * </p>
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class LowMcSoprpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3897214217963851629L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LOWMC_SOPRP";

    /**
     * singleton mode
     */
    private static final LowMcSoprpPtoDesc INSTANCE = new LowMcSoprpPtoDesc();

    /**
     * private constructor
     */
    private LowMcSoprpPtoDesc() {
        // empty
    }

    public enum PrpSteps {
        /**
         * generate encryption keys
         */
        GET_KEY,
        /**
         * initialize the matrix for encryption
         */
        INIT_MATRIX,
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

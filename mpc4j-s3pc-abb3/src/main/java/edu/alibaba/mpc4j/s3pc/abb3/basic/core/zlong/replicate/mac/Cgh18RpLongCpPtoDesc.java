package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGH+18 Zl64 circuit protocol description. The protocol is presented in the following paper:
 * <p>
 * Koji Chida, Daniel Genkin, Koki Hamada, Dai Ikarashi, Ryo Kikuchi, Yehuda Lindell, and Ariel Nof
 * Fast large-scale honest-majority MPC for malicious adversaries
 * Advances in Cryptology – CRYPTO 2018 pp 34–64
 * </p>
 *
 * @author Feng Han
 * @date 2024/01/09
 */
public class Cgh18RpLongCpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4198127504152062124L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGH^+18_AC";

    /**
     * protocol step
     */
    public enum PtoStep {
        /**
         * initialize
         */
        INIT,
        /**
         * input the share
         */
        INPUT_SHARE,
        /**
         * reveal the share to the specific party
         */
        REVEAL_SHARE,
        /**
         * open the share to all parties
         */
        OPEN_SHARE,
        /**
         * and operation
         */
        MUL_OP,
        /**
         * compare view
         */
        COMPARE_VIEW,
    }

    /**
     * singleton mode
     */
    private static final Cgh18RpLongCpPtoDesc INSTANCE = new Cgh18RpLongCpPtoDesc();

    /**
     * private constructor
     */
    private Cgh18RpLongCpPtoDesc() {
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

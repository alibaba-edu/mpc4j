package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Information of aby3 zl64c protocols
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3LongCpPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -5751686212146649124L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ABY3_AC";

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
    private static final Aby3LongCpPtoDesc INSTANCE = new Aby3LongCpPtoDesc();

    /**
     * private constructor
     */
    private Aby3LongCpPtoDesc() {
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

package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GP23 Zl Corr protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Gp23ZlCorrPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7013525486695334284L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GP23_ZL_CORR";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender send s
         */
        SENDER_SENDS_S,
    }

    /**
     * singleton mode
     */
    private static final Gp23ZlCorrPtoDesc INSTANCE = new Gp23ZlCorrPtoDesc();

    /**
     * private constructor.
     */
    private Gp23ZlCorrPtoDesc() {
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

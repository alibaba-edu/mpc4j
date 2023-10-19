package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.gp23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * GP23 Zl Truncation protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class Gp23ZlTruncPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1234131234561231234L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GP23_ZL_TRUNC";

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
    private static final Gp23ZlTruncPtoDesc INSTANCE = new Gp23ZlTruncPtoDesc();

    /**
     * private constructor.
     */
    private Gp23ZlTruncPtoDesc() {
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

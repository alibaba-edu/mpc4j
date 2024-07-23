package edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Edit distance protocol description
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public class S2pcDiagEditDistPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4055163853223322134L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "S2PC_EDIT_EDIT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * Send str length.
         */
        SEND_CHAR_LEN,
        /**
         * Send result.
         */
        SEND_RESULT,
    }

    /**
     * singleton mode
     */
    private static final S2pcDiagEditDistPtoDesc INSTANCE = new S2pcDiagEditDistPtoDesc();

    /**
     * private constructor
     */
    private S2pcDiagEditDistPtoDesc() {
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
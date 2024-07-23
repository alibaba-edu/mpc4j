package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * LLL24 Decision OSN protocol description.
 *
 * @author Feng Han
 * @date 2024/6/20
 */
public class Lll24DosnPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1245784250526289922L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLL24_DOSN";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends m = x + a^(1)
         */
        SENDER_SEND_MASK_INPUT,
    }

    /**
     * private constructor.
     */
    private Lll24DosnPtoDesc() {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Lll24DosnPtoDesc INSTANCE = new Lll24DosnPtoDesc();

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

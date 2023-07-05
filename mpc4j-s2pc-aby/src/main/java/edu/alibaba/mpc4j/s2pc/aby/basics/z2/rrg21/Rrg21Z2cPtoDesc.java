package edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRG+21 Z2 circuit protocol description. This protocol implements AND using the mux technique presented in
 * Appendix A of the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Rahul Kranti Kiran Goli, Divya Gupta, Rahul Sharma, Nishanth Chandran, and
 * Aseem Rastogi. Sirnn: A math library for secure rnn inference. S&P 2021, pp. 1003-1020. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
class Rrg21Z2cPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7737390913072031268L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRG21_BC";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends the share
         */
        SENDER_SEND_INPUT_SHARE,
        /**
         * the receiver sends the share
         */
        RECEIVER_SEND_INPUT_SHARE,
        /**
         * the sender sends the correlation
         */
        SENDER_SEND_DELTA0,
        /**
         * the receiver sends the correlation
         */
        RECEIVER_SEND_DELTA1,
        /**
         * the sender sends the output share
         */
        SENDER_SEND_OUTPUT_SHARE,
        /**
         * the receiver sends the output share
         */
        RECEIVER_SEND_OUTPUT_SHARE
    }

    /**
     * singleton mode
     */
    private static final Rrg21Z2cPtoDesc INSTANCE = new Rrg21Z2cPtoDesc();

    /**
     * private constructor
     */
    private Rrg21Z2cPtoDesc() {
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

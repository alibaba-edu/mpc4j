package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRKG+21 Zl mux protocol description. The protocol comes from Appendix A of the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Rahul Kranti Kiran Goli, Divya Gupta, Rahul Sharma, Nishanth Chandran, and
 * Aseem Rastogi. Sirnn: A math library for secure rnn inference. S&P 2021, pp. 1003-1020. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class Rrg21ZlMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3797353173620043078L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRG+21_ZL_MUX";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends the correlation
         */
        SENDER_SEND_DELTA0,
        /**
         * the receiver sends the correlation
         */
        RECEIVER_SEND_DELTA1,
    }

    /**
     * singleton mode
     */
    private static final Rrg21ZlMuxPtoDesc INSTANCE = new Rrg21ZlMuxPtoDesc();

    /**
     * private constructor.
     */
    private Rrg21ZlMuxPtoDesc() {
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

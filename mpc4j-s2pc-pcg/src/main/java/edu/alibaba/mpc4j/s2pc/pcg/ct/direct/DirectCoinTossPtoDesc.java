package edu.alibaba.mpc4j.s2pc.pcg.ct.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct coin-tossing protocol description. The sender directly sends the randomness to the client. Note that this
 * protocol is secure against semi-honest adversary.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
class DirectCoinTossPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2669143792810927254L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_COIN_TOSS";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends coins
         */
        SENDER_SEND_COINS,
    }

    /**
     * singleton mode
     */
    private static final DirectCoinTossPtoDesc INSTANCE = new DirectCoinTossPtoDesc();

    /**
     * private constructor.
     */
    private DirectCoinTossPtoDesc() {
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

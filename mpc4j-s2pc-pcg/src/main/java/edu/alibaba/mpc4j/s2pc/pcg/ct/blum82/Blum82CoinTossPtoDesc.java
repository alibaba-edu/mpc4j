package edu.alibaba.mpc4j.s2pc.pcg.ct.blum82;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Blum82 coin-tossing protocol. The protocol is described in the following paper:
 * <p>
 * Blum, Manuel. Coin flipping by phone. In COMPCON, pp. 133-137. 1982.
 * </p>
 * The security proof is shown in the following paper:
 * <p>
 * Lindell, Yehuda. How to simulate it: a tutorial on the simulation proof technique. Tutorials on the Foundations of
 * Cryptography: Dedicated to Oded Goldreich (2017): 277-346.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
class Blum82CoinTossPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7996797474462887817L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BLUM82_COIN_TOSS";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sender sends the commitment
         */
        SENDER_SEND_COMMITMENT,
        /**
         * receiver sends the commitment
         */
        RECEIVER_SEND_COMMITMENT,
        /**
         * sender sends coins
         */
        SENDER_SEND_COINS,
        /**
         * receiver sends coins
         */
        RECEIVER_SEND_COINS,
    }

    /**
     * singleton mode
     */
    private static final Blum82CoinTossPtoDesc INSTANCE = new Blum82CoinTossPtoDesc();

    /**
     * private constructor.
     */
    private Blum82CoinTossPtoDesc() {
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

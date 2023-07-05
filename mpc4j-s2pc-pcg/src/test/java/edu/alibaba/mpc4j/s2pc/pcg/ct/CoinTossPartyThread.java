package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * coin-tossing protocol party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
class CoinTossPartyThread extends Thread {
    /**
     * party
     */
    private final CoinTossParty party;
    /**
     * num
     */
    private final int num;
    /**
     * bit length
     */
    private final int bitLength;
    /**
     * coins
     */
    private byte[][] coins;

    CoinTossPartyThread(CoinTossParty party, int num, int bitLength) {
        this.party = party;
        this.num = num;
        this.bitLength = bitLength;
    }

    byte[][] getPartyOutput() {
        return coins;
    }

    @Override
    public void run() {
        try {
            party.init();
            coins = party.coinToss(num, bitLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
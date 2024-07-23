package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * F_3 -> F_2 modulus conversion sender.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
class Conv32PartyThread extends Thread {
    /**
     * sender
     */
    private final Conv32Party party;
    /**
     * update num
     */
    private final int expectNum;
    /**
     * wi
     */
    private final byte[] wi;
    /**
     * party output
     */
    private byte[] partyOutput;

    Conv32PartyThread(Conv32Party party, byte[] wi) {
        this(party, wi.length, wi);
    }

    Conv32PartyThread(Conv32Party party, int expectNum, byte[] wi) {
        this.party = party;
        this.expectNum = expectNum;
        this.wi = wi;
    }

    byte[] getPartyOutput() {
        return partyOutput;
    }

    @Override
    public void run() {
        try {
            party.init(expectNum);
            partyOutput = party.conv(wi);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

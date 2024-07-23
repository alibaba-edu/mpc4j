package edu.alibaba.mpc4j.common.rpc.main;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main Party 1 thread.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public class MainParty1Thread extends Thread {
    /**
     * RPC for Party 1
     */
    private final Rpc party1Rpc;
    /**
     * Party 2
     */
    private final Party party2;
    /**
     * main 2PC protocol
     */
    private final MainTwoPartyPto mainTwoPartyPto;
    /**
     * success
     */
    private boolean success;

    public MainParty1Thread(Rpc party1Rpc, Party party2, MainTwoPartyPto mainTwoPartyPto) {
        this.party1Rpc = party1Rpc;
        this.party2 = party2;
        this.mainTwoPartyPto = mainTwoPartyPto;
        success = false;
    }

    public boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            mainTwoPartyPto.runParty1(party1Rpc, party2);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
    }
}

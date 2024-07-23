package edu.alibaba.mpc4j.common.rpc.main;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main Party 2 thread.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public class MainParty2Thread extends Thread {
    /**
     * RPC for Party 2
     */
    private final Rpc party2Rpc;
    /**
     * Party 1
     */
    private final Party party1;
    /**
     * main 2PC protocol
     */
    private final MainTwoPartyPto mainTwoPartyPto;
    /**
     * success
     */
    private boolean success;

    public MainParty2Thread(Rpc party2Rpc, Party party1, MainTwoPartyPto mainTwoPartyPto) {
        this.party2Rpc = party2Rpc;
        this.party1 = party1;
        this.mainTwoPartyPto = mainTwoPartyPto;
        success = false;
    }

    public boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            mainTwoPartyPto.runParty2(party2Rpc, party1);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
    }
}

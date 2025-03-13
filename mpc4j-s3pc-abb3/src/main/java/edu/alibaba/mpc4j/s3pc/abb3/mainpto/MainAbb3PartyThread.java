package edu.alibaba.mpc4j.s3pc.abb3.mainpto;

import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main Party 1 thread.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public class MainAbb3PartyThread extends Thread {
    /**
     * own rpc
     */
    private final Rpc ownRpc;
    /**
     * main Abb3 protocol
     */
    private final MainAbb3PartyPto mainAbb3PartyPto;
    /**
     * success
     */
    private boolean success;

    public MainAbb3PartyThread(Rpc ownRpc, MainAbb3PartyPto mainAbb3PartyPto) {
        this.ownRpc = ownRpc;
        this.mainAbb3PartyPto = mainAbb3PartyPto;
        success = false;
    }

    public boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            mainAbb3PartyPto.runParty(ownRpc);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
    }
}

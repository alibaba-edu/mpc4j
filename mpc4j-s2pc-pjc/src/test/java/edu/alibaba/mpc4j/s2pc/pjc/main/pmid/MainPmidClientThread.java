package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main PMID client thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPmidClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PID
     */
    private final PmidMain pmidMain;

    MainPmidClientThread(Rpc clientRpc, Party serverParty, PmidMain pmidMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.pmidMain = pmidMain;
    }

    @Override
    public void run() {
        try {
            pmidMain.runClient(clientRpc, serverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
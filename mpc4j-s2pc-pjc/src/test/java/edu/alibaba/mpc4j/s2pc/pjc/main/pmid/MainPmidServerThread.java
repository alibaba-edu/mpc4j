package edu.alibaba.mpc4j.s2pc.pjc.main.pmid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main PMID server thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPmidServerThread extends Thread {
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main PMID
     */
    private final PmidMain pmidMain;

    MainPmidServerThread(Rpc serverRpc, Party clientParty, PmidMain pmidMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.pmidMain = pmidMain;
    }

    @Override
    public void run() {
        try {
            pmidMain.runServer(serverRpc, clientParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
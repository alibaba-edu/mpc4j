package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main PSU client thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPsuClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PSU
     */
    private final PsuMain psuMain;

    MainPsuClientThread(Rpc clientRpc, Party serverParty, PsuMain psuMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.psuMain = psuMain;
    }

    @Override
    public void run() {
        try {
            psuMain.runClient(clientRpc, serverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
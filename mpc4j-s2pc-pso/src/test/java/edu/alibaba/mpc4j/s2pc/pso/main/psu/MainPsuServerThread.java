package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * main PSU server thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPsuServerThread extends Thread {
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main PSU
     */
    private final PsuMain psuMain;

    MainPsuServerThread(Rpc serverRpc, Party clientParty, PsuMain psuMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.psuMain = psuMain;
    }

    @Override
    public void run() {
        try {
            psuMain.runServer(serverRpc, clientParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
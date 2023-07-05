package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.PidMain;

/**
 * main PID client thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPidClientThread extends Thread {
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
    private final PidMain pidMain;

    MainPidClientThread(Rpc clientRpc, Party serverParty, PidMain pidMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.pidMain = pidMain;
    }

    @Override
    public void run() {
        try {
            pidMain.runClient(clientRpc, serverParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
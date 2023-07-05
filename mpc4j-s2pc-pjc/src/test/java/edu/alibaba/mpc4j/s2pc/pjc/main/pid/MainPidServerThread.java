package edu.alibaba.mpc4j.s2pc.pjc.main.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.PidMain;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;

import java.util.Set;

/**
 * main PID server thread.
 *
 * @author Weiran Liu
 * @date 2023/6/30
 */
class MainPidServerThread extends Thread {
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main PID
     */
    private final PidMain pidMain;

    MainPidServerThread(Rpc serverRpc, Party clientParty, PidMain pidMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.pidMain = pidMain;
    }

    @Override
    public void run() {
        try {
            pidMain.runServer(serverRpc, clientParty);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
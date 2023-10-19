package edu.alibaba.mpc4j.s2pc.pso.main.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * CCPSI main server thread.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
public class MainCcpsiServerThread extends Thread {
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
    private final CcpsiMain ccpsiMain;
    /**
     * success
     */
    private boolean success;

    MainCcpsiServerThread(Rpc serverRpc, Party clientParty, CcpsiMain ccpsiMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.ccpsiMain = ccpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            ccpsiMain.runServer(serverRpc, clientParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}

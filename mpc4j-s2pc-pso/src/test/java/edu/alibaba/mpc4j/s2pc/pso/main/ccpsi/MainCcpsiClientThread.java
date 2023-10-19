package edu.alibaba.mpc4j.s2pc.pso.main.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * CCPSI main client thread.
 *
 * @author Feng Han
 * @date 2023/10/10
 */
public class MainCcpsiClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main PSI
     */
    private final CcpsiMain ccpsiMain;
    /**
     * success
     */
    private boolean success;

    MainCcpsiClientThread(Rpc clientRpc, Party serverParty, CcpsiMain ccpsiMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.ccpsiMain = ccpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            ccpsiMain.runClient(clientRpc, serverParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}

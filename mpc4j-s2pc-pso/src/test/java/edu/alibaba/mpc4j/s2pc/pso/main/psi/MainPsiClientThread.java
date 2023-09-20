package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * PSI main client thread.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class MainPsiClientThread extends Thread {
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
    private final PsiMain psiMain;
    /**
     * success
     */
    private boolean success;

    MainPsiClientThread(Rpc clientRpc, Party serverParty, PsiMain psiMain) {
        this.clientRpc = clientRpc;
        this.serverParty = serverParty;
        this.psiMain = psiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            psiMain.runClient(clientRpc, serverParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}

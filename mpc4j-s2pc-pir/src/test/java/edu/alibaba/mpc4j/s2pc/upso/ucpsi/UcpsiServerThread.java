package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * unbalanced circuit PSI server thread.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiServerThread extends Thread {
    /**
     * the server
     */
    private final UcpsiServer server;
    /**
     * the server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * the server output
     */
    private SquareZ2Vector serverOutput;

    UcpsiServerThread(UcpsiServer server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    SquareZ2Vector getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet, clientElementSize);
            serverOutput = server.psi();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

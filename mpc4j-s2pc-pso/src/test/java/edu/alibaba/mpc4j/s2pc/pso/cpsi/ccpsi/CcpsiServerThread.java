package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * client-payload circuit PSI server thread.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class CcpsiServerThread extends Thread {
    /**
     * server
     */
    private final CcpsiServer<ByteBuffer> server;
    /**
     * server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * server output
     */
    private SquareZ2Vector serverOutput;

    CcpsiServerThread(CcpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
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
            server.init(serverElementSet.size(), clientElementSize);
            serverOutput = server.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

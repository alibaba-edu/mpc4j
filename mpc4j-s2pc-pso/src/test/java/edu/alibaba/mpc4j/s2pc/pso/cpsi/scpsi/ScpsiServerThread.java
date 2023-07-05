package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * server-payload circuit PSI server thread.
 *
 * @author Liqiang Peng
 * @date 2023/2/1
 */
public class ScpsiServerThread extends Thread {
    /**
     * server
     */
    private final ScpsiServer<ByteBuffer> server;
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
    private ScpsiServerOutput<ByteBuffer> serverOutput;

    ScpsiServerThread(ScpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    ScpsiServerOutput<ByteBuffer> getServerOutput() {
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

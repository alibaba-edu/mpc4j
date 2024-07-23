package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT server thread.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class MqRpmtServerThread extends Thread {
    /**
     * mqRPMT server
     */
    private final MqRpmtServer server;
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
    private ByteBuffer[] serverOutput;

    MqRpmtServerThread(MqRpmtServer server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    ByteBuffer[] getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            serverOutput = server.mqRpmt(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

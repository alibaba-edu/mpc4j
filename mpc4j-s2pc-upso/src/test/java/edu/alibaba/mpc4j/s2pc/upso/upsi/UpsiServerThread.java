package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiServer;

import java.io.IOException;
import java.util.Set;

/**
 * UPSI server thread.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiServerThread<T> extends Thread {
    /**
     * UPSI server
     */
    private final UpsiServer<T> server;
    /**
     * server element set
     */
    private final Set<T> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * max client element size
     */
    private final int maxClientElementSize;

    UpsiServerThread(UpsiServer<T> server, int maxClientElementSize, Set<T> serverElementSet, int clientElementSize) {
        this.server = server;
        this.maxClientElementSize = maxClientElementSize;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(maxClientElementSize);
            server.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiServer;

import java.util.Set;

/**
 * CMG21 UPSI server thread.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class Cmg21UpsiServerThread<T> extends Thread {
    /**
     * CMG21 UPSI server
     */
    private final Cmg21UpsiServer<T> server;
    /**
     * UPSI config
     */
    private final Cmg21UpsiParams upsiParams;
    /**
     * server element set
     */
    private final Set<T> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;

    Cmg21UpsiServerThread(Cmg21UpsiServer<T> server, Cmg21UpsiParams upsiParams, Set<T> serverElementSet,
                          int clientElementSize) {
        this.server = server;
        this.upsiParams = upsiParams;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(upsiParams);
            server.psi(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

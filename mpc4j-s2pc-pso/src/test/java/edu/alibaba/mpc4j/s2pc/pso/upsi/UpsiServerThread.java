package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * 非平衡PSI协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiServerThread<T> extends Thread {
    /**
     * 服务端
     */
    private final UpsiServer<T> server;
    /**
     * UPSI协议配置项
     */
    private final UpsiParams upsiParams;
    /**
     * 服务端集合
     */
    private final Set<T> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;

    UpsiServerThread(UpsiServer<T> server, UpsiParams upsiParams, Set<T> serverElementSet, int clientElementSize) {
        this.server = server;
        this.upsiParams = upsiParams;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.getRpc().connect();
            server.init(upsiParams);
            server.getRpc().synchronize();
            server.psi(serverElementSet, clientElementSize);
            server.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.upso.upsi;

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
     * 服务端集合
     */
    private final Set<T> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;
    /**
     * 客户端最大元素数量
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
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

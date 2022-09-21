package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI协议服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class PsiServerThread extends Thread {
    /**
     * PSI服务端
     */
    private final PsiServer<ByteBuffer> psiServer;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;

    PsiServerThread(PsiServer<ByteBuffer> psiServer, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.psiServer = psiServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            psiServer.getRpc().connect();
            psiServer.init(serverElementSet.size(), clientElementSize);
            psiServer.psi(serverElementSet, clientElementSize);
            psiServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

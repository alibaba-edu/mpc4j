package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI协议客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class PsiClientThread extends Thread {
    /**
     * PSI客户端
     */
    private final PsiClient<ByteBuffer> psiClient;
    /**
     * 客户端集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 客户端交集
     */
    private Set<ByteBuffer> intersectionSet;

    PsiClientThread(PsiClient<ByteBuffer> psiClient, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.psiClient = psiClient;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            psiClient.getRpc().connect();
            psiClient.init(clientElementSet.size(), serverElementSize);
            intersectionSet = psiClient.psi(clientElementSet, serverElementSize);
            psiClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

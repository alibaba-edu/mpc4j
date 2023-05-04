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
    private final PsiClient<ByteBuffer> client;
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

    PsiClientThread(PsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            intersectionSet = client.psi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

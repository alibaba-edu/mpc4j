package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU协议客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
class PsuClientThread extends Thread {
    /**
     * PSU客户端
     */
    private final PsuClient client;
    /**
     * 客户端集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;
    /**
     * 客户端并集
     */
    private Set<ByteBuffer> unionSet;

    PsuClientThread(PsuClient client, Set<ByteBuffer> clientElementSet, int serverElementSize,
                    int elementByteLength) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
        this.elementByteLength = elementByteLength;
    }

    Set<ByteBuffer> getUnionSet() {
        return unionSet;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            unionSet = client.psu(clientElementSet, serverElementSize, elementByteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU协议服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
class PsuServerThread extends Thread {
    /**
     * PSU服务端
     */
    private final PsuServer psuServer;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;
    /**
     * 元素字节长度
     */
    private final int elementByteLength;

    PsuServerThread(PsuServer psuServer, Set<ByteBuffer> serverElementSet, int clientElementSize,
        int elementByteLength) {
        this.psuServer = psuServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
        this.elementByteLength = elementByteLength;
    }

    @Override
    public void run() {
        try {
            psuServer.getRpc().connect();
            psuServer.init(serverElementSet.size(), clientElementSize);
            psuServer.psu(serverElementSet, clientElementSize, elementByteLength);
            psuServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

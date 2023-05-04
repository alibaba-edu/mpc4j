package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT协议服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class MqRpmtServerThread extends Thread {
    /**
     * mqRPMT服务端
     */
    private final MqRpmtServer server;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;
    /**
     * 服务端输出向量
     */
    private ByteBuffer[] serverVector;

    MqRpmtServerThread(MqRpmtServer server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    ByteBuffer[] getServerVector() {
        return serverVector;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), clientElementSize);
            serverVector = server.mqRpmt(serverElementSet, clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

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
    private final MqRpmtServer mqRpmtServer;
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

    MqRpmtServerThread(MqRpmtServer mqRpmtServer, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.mqRpmtServer = mqRpmtServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    ByteBuffer[] getServerVector() {
        return serverVector;
    }

    @Override
    public void run() {
        try {
            mqRpmtServer.getRpc().connect();
            mqRpmtServer.init(serverElementSet.size(), clientElementSize);
            serverVector = mqRpmtServer.mqRpmt(serverElementSet, clientElementSize);
            mqRpmtServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

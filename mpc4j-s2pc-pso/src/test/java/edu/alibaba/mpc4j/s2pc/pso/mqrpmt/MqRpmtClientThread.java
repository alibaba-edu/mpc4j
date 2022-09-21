package edu.alibaba.mpc4j.s2pc.pso.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT协议客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class MqRpmtClientThread extends Thread {
    /**
     * mqRPMT客户端
     */
    private final MqRpmtClient mqRpmtClient;
    /**
     * 客户端集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 客户端输出
     */
    private boolean[] containVector;

    MqRpmtClientThread(MqRpmtClient mqRpmtClient, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.mqRpmtClient = mqRpmtClient;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    boolean[] getContainVector() {
        return containVector;
    }

    @Override
    public void run() {
        try {
            mqRpmtClient.getRpc().connect();
            mqRpmtClient.init(clientElementSet.size(), serverElementSize);
            containVector = mqRpmtClient.mqRpmt(clientElementSet, serverElementSize);
            mqRpmtClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class PsuClientThread extends Thread {
    /**
     * PSU接收方
     */
    private final PsuClient psuClient;
    /**
     * 接收方集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 发送方元素数量
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

    PsuClientThread(PsuClient psuClient, Set<ByteBuffer> clientElementSet, int serverElementSize,
        int elementByteLength) {
        this.psuClient = psuClient;
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
            psuClient.getRpc().connect();
            psuClient.init(clientElementSet.size(), serverElementSize);
            unionSet = psuClient.psu(clientElementSet, serverElementSize, elementByteLength);
            psuClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * mqRPMT client thread.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class MqRpmtClientThread extends Thread {
    /**
     * mqRPMT client
     */
    private final MqRpmtClient client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * client output
     */
    private boolean[] clientOutput;

    MqRpmtClientThread(MqRpmtClient client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    boolean[] getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            clientOutput = client.mqRpmt(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

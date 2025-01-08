package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * offline-online PSUЭ��ͻ����̡߳�
 *
 * @author Feng Han
 * @date 2024/12/9
 */
public class OoPsuClientThread extends Thread {
    /**
     * PSU client
     */
    private final OoPsuClient client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server set size
     */
    private final int serverElementSize;
    /**
     * element byte length
     */
    private final int elementByteLength;
    /**
     * client output
     */
    private PsuClientOutput clientOutput;

    OoPsuClientThread(OoPsuClient client, Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
        this.elementByteLength = elementByteLength;
    }

    PsuClientOutput getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            client.preCompute(clientElementSet.size(), serverElementSize, elementByteLength);
            clientOutput = client.psu(clientElementSet, serverElementSize, elementByteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

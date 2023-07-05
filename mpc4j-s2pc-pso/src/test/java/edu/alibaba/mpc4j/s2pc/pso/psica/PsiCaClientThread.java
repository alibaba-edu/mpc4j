package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI Cardinality client thread.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
class PsiCaClientThread extends Thread {
    /**
     * client
     */
    private final PsiCaClient<ByteBuffer> client;
    /**
     * client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * cardinality
     */
    private int cardinality;

    PsiCaClientThread(PsiCaClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    int getCardinality() {
        return cardinality;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            cardinality = client.psiCardinality(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}


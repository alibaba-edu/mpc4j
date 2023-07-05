package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * unbalanced circuit PSI client thread.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiClientThread extends Thread {
    /**
     * the client
     */
    private final UcpsiClient<ByteBuffer> client;
    /**
     * the client element set
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * server element size
     */
    private final int serverElementSize;
    /**
     * the client output
     */
    private UcpsiClientOutput<ByteBuffer> clientOutput;

    UcpsiClientThread(UcpsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    UcpsiClientOutput<ByteBuffer> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), serverElementSize);
            clientOutput = client.psi(clientElementSet);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

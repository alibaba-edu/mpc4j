package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiClient;

import java.util.Set;


/**
 * UPSI client thread.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiClientThread<T> extends Thread {
    /**
     * UPSI client
     */
    private final UpsiClient<T> client;
    /**
     * client element set
     */
    private final Set<T> clientElementSet;
    /**
     * intersection set
     */
    private Set<T> intersectionSet;
    /**
     * max client element size
     */
    private final int maxClientElementSize;

    UpsiClientThread(UpsiClient<T> client, int maxClientElementSize, Set<T> clientElementSet) {
        this.client = client;
        this.maxClientElementSize = maxClientElementSize;
        this.clientElementSet = clientElementSet;
    }

    Set<T> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(maxClientElementSize);
            intersectionSet = client.psi(clientElementSet);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

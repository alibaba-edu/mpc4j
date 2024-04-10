package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;

import java.util.Set;


/**
 * CMG21 UPSI client thread.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class Cmg21UpsiClientThread<T> extends Thread {
    /**
     * CMG21 UPSI client
     */
    private final Cmg21UpsiClient<T> client;
    /**
     * UPSI config
     */
    private final Cmg21UpsiParams upsiParams;
    /**
     * client element set
     */
    private final Set<T> clientElementSet;
    /**
     * intersection set
     */
    private Set<T> intersectionSet;

    Cmg21UpsiClientThread(Cmg21UpsiClient<T> client, Cmg21UpsiParams upsiParams, Set<T> clientElementSet) {
        this.client = client;
        this.upsiParams = upsiParams;
        this.clientElementSet = clientElementSet;
    }

    Set<T> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(upsiParams);
            intersectionSet = client.psi(clientElementSet);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

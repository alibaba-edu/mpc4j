package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;

import java.util.Set;


/**
 * 非平衡PSI协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class Cmg21UpsiClientThread<T> extends Thread {
    /**
     * 客户端
     */
    private final UpsiClient<T> client;
    /**
     * UPSI协议配置项
     */
    private final UpsiParams upsiParams;
    /**
     * 客户端集合
     */
    private final Set<T> clientElementSet;
    /**
     * 交集
     */
    private Set<T> intersectionSet;

    Cmg21UpsiClientThread(UpsiClient<T> client, UpsiParams upsiParams, Set<T> clientElementSet) {
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

package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;


/**
 * 非平衡PSI协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiClientThread<T> extends Thread {
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

    UpsiClientThread(UpsiClient<T> client, UpsiParams upsiParams, Set<T> clientElementSet) {
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
            client.getRpc().connect();
            client.init(upsiParams);
            client.getRpc().synchronize();
            intersectionSet = client.psi(clientElementSet);
            client.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

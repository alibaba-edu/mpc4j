package edu.alibaba.mpc4j.s2pc.upso.upsi;

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
     * 客户端集合
     */
    private final Set<T> clientElementSet;
    /**
     * 交集
     */
    private Set<T> intersectionSet;
    /**
     * 客户端最大元素数量
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

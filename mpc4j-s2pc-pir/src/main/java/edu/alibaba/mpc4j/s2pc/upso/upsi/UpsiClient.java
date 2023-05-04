package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * 非平衡PSI协议客户端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiClient<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param upsiParams 非平衡PSI协议参数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(UpsiParams upsiParams) throws MpcAbortException;

    /**
     * 初始化协议。
     *
     * @param maxClientElementSize 客户端最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxClientElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementSet 客户端元素集合。
     * @return 交集结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Set<T> psi(Set<T> clientElementSet) throws MpcAbortException;
}

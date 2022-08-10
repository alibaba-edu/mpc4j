package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiFactory.UpsiType;

import java.util.Set;

/**
 * 非平衡PSI协议客户端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public interface UpsiClient<T> extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    UpsiType getPtoType();

    /**
     * 初始化协议。
     *
     * @param upsiParams 非平衡PSI协议参数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(UpsiParams upsiParams) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementSet 客户端元素集合。
     * @return 交集结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Set<T> psi(Set<T> clientElementSet) throws MpcAbortException;
}

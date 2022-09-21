package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI协议服务端接口。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public interface PsiServer<T> extends TwoPartyPto, SecurePto {
    /**
     * 返回PSI协议类型。
     *
     * @return PSI协议类型。
     */
    @Override
    PsiFactory.PsiType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxServerElementSize   服务端最大元素数量。
     * @param maxClientElementSize 客户端最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet    服务端元素集合。
     * @param clientElementSize 客户端元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}

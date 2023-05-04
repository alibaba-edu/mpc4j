package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Map;
import java.util.Set;

/**
 * PMID协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidClient<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxClientSetSize 客户端最大元素数量。
     * @param maxClientU       客户端最大重数上界。
     * @param maxServerSetSize 服务端最大元素数量。
     * @param maxServerU       服务端最大重数上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementSet 客户端元素集合。
     * @param serverSetSize    服务端元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementMap 客户端元素与重数映射。
     * @param serverSetSize    服务端元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementSet 客户端元素集合。
     * @param serverSetSize    服务端元素数量。
     * @param serverU          服务端重数上界。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize, int serverU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementMap 客户端元素与重数映射。
     * @param serverSetSize    服务端元素数量。
     * @param serverU          服务端重数上界。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize, int serverU) throws MpcAbortException;
}

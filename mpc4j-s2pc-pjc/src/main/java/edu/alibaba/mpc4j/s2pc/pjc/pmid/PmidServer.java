package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Map;
import java.util.Set;

/**
 * PMID服务端。
 *
 * @author Weiran Liu
 * @date 2022/5/6
 */
public interface PmidServer<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxServerSetSize 服务端集合最大元素数量。
     * @param maxServerU       服务端重数上界。
     * @param maxClientSetSize 客户端集合最大元素数量。
     * @param maxClientU       客户端最重数上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxServerSetSize, int maxServerU, int maxClientSetSize, int maxClientU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet 服务端元素集合。
     * @param clientSetSize    客户端元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementSet 服务端元素集合。
     * @param clientSetSize    客户端元素数量。
     * @param clientU          客户端重数上界。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Set<T> serverElementSet, int clientSetSize, int clientU) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementMap 客户端元素与重数映射。
     * @param clientSetSize    客户端元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param serverElementMap 客户端元素与重数映射。
     * @param clientSetSize    客户端元素数量。
     * @param clientU          客户端重数上界。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PmidPartyOutput<T> pmid(Map<T, Integer> serverElementMap, int clientSetSize, int clientU) throws MpcAbortException;
}

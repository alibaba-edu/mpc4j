package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * 关键词索引PIR协议客户端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirClient<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param kwPirParams     关键字PIR参数。
     * @param labelByteLength 标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(KwPirParams kwPirParams, int labelByteLength) throws MpcAbortException;

    /**
     * 初始化协议。
     *
     * @param maxRetrievalSize 最大检索数量。
     * @param labelByteLength  标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRetrievalSize, int labelByteLength) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param retrievalSet 检索集合。
     * @return 查询元素和标签映射。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Map<T, ByteBuffer> pir(Set<T> retrievalSet) throws MpcAbortException;
}

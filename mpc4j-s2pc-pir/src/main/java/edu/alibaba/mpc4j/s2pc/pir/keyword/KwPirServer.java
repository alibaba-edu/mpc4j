package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 关键词索引PIR协议服务端接口。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirServer<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param kwPirParams     关键词PIR参数。
     * @param keywordLabelMap 关键字和标签映射。
     * @param labelByteLength 标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(KwPirParams kwPirParams, Map<T, ByteBuffer> keywordLabelMap, int labelByteLength) throws MpcAbortException;

    /**
     * 初始化协议。
     *
     * @param keywordLabelMap  关键字和标签映射。
     * @param maxRetrievalSize 最大检索数量。
     * @param labelByteLength  标签字节长度。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(Map<T, ByteBuffer> keywordLabelMap, int maxRetrievalSize, int labelByteLength) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void pir() throws MpcAbortException;
}

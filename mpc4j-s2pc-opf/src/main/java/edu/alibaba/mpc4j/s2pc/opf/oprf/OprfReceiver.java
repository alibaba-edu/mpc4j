package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * OPRF协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public interface OprfReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    default void init(int maxBatchSize) throws MpcAbortException {
        init(maxBatchSize, maxBatchSize);
    }

    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @param maxPrfNum    PRF最大调用次数。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param inputs 输入数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    OprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException;
}

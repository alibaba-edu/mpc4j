package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * MPOPRF发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public interface MpOprfSender extends OprfSender {
    /**
     * 执行协议。
     *
     * @param batchSize 批处理数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    @Override
    MpOprfSenderOutput oprf(int batchSize) throws MpcAbortException;
}

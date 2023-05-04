package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * MPOPRF接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public interface MpOprfReceiver extends OprfReceiver {
    /**
     * 执行协议。
     *
     * @param inputs 输入数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    @Override
    MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException;
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * 预计算COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface PreCotReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param preReceiverOutput 预计算接收方输出。
     * @param choices 选择比特数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotReceiverOutput receive(CotReceiverOutput preReceiverOutput, boolean[] choices) throws MpcAbortException;
}

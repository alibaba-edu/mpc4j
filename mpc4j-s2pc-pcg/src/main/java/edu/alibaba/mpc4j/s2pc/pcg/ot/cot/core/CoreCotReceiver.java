package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory.CoreCotType;

/**
 * 核COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface CoreCotReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择比特数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotReceiverOutput receive(boolean[] choices) throws MpcAbortException;
}

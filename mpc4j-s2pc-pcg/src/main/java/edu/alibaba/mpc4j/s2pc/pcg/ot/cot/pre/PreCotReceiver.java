package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory.PreCotType;

/**
 * 预计算COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface PreCotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    PreCotType getPtoType();

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

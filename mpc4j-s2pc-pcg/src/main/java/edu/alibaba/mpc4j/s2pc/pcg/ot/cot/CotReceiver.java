package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotReceiver extends TwoPartyPto, SecurePto {

    @Override
    CotFactory.CotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   最大更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择比特数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotReceiverOutput receive(boolean[] choices) throws MpcAbortException;
}

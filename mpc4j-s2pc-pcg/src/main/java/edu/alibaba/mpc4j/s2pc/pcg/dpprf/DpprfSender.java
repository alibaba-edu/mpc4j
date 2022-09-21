package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * DPPRF协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface DpprfSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    DpprfFactory.DpprfType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxBatchNum   最大批处理数量。
     * @param maxAlphaBound 最大α上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchNum, int maxAlphaBound) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum   批处理数量。
     * @param alphaBound α上界。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    DpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum        批处理数量。
     * @param alphaBound      α上界。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    DpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}

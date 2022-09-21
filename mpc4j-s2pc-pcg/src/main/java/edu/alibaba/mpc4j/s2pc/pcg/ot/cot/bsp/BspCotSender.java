package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * BSP-COT发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BspCotSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    BspCotFactory.BspCotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param delta       关联值Δ。
     * @param maxBatchNum 最大批处理数量。
     * @param maxNum      最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(byte[] delta, int maxBatchNum, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum 批处理数量。
     * @param num      数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotSenderOutput send(int batchNum, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum        批处理数量。
     * @param num             数量。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotSenderOutput send(int batchNum, int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}

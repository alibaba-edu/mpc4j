package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * BSP-COT接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BspCotReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBatchNum 最大批处理数量。
     * @param maxNum      最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchNum, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray α数组。
     * @param num        数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray        α数组。
     * @param num               数量。
     * @param preReceiverOutput 预计算接收方输出。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}

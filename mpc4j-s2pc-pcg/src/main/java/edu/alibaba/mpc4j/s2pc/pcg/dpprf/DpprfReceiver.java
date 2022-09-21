package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * DPPRF协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface DpprfReceiver extends TwoPartyPto, SecurePto {
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
     * @param alphaArray α数组。
     * @param alphaBound α上界。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    DpprfReceiverOutput puncture(int[] alphaArray, int alphaBound) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray        α数组。
     * @param alphaBound        α上界。
     * @param preReceiverOutput 预计算接收方输出。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    DpprfReceiverOutput puncture(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;

/**
 * MSP-COT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface MspCotSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    MspCotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param delta  关联值Δ。
     * @param maxT   最大稀疏点数量。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param t   稀疏点数量。
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    MspCotSenderOutput send(int t, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param t               稀疏点数量。
     * @param num             数量。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}

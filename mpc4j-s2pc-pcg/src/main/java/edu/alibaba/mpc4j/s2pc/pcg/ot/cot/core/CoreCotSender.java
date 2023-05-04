package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * 核COT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface CoreCotSender extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param delta  关联值Δ。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotSenderOutput send(int num) throws MpcAbortException;
}

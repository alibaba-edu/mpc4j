package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * COT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotSender extends TwoPartyPto, SecurePto {

    @Override
    CotFactory.CotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param delta       关联值Δ。
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(byte[] delta, int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotSenderOutput send(int num) throws MpcAbortException;
}

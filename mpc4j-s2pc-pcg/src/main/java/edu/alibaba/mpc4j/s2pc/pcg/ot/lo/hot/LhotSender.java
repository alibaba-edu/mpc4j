package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotSender;

/**
 * 2^l选1-HOT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface LhotSender extends LotSender {

    /**
     * 初始化协议。
     *
     * @param inputBitLength      输入比特长度。
     * @param delta  关联值Δ。
     * @param maxNum 最大执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, byte[] delta, int maxNum) throws MpcAbortException;

    @Override
    LhotSenderOutput send(int num) throws MpcAbortException;
}

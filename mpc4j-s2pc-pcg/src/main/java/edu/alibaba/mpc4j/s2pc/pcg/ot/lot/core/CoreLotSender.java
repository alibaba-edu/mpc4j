package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 核2^l选1-OT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface CoreLotSender extends TwoPartyPto, SecurePto {
    /**
     * 初始化协议。
     *
     * @param inputBitLength 输入比特长度。
     * @param delta          关联值Δ。
     * @param maxNum         最大执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, byte[] delta, int maxNum) throws MpcAbortException;

    /**
     * 初始化协议。
     *
     * @param inputBitLength 输入比特长度。
     * @param maxNum         最大执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 执行数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CoreLotSenderOutput send(int num) throws MpcAbortException;
}

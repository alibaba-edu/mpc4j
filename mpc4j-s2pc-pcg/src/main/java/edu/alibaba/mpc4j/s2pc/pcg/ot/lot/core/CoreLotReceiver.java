package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;

/**
 * 核2^l选1-LOT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface CoreLotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 初始化协议。
     *
     * @param inputBitLength 选择值比特长度。
     * @param maxNum         最大执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择值数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    LotReceiverOutput receive(byte[][] choices) throws MpcAbortException;
}

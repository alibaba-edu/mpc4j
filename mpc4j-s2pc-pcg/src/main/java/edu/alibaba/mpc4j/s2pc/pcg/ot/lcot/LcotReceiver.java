package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 2^l选1-COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface LcotReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param inputBitLength 选择值比特长度。
     * @param maxNum         最大执行数量。
     * @return output bit length.
     * @throws MpcAbortException 如果协议异常中止。
     */
    int init(int inputBitLength, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择值数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    LcotReceiverOutput receive(byte[][] choices) throws MpcAbortException;
}

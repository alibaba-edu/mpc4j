package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotFactory.LotType;

/**
 * 2^l选1-OT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public interface LotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回2^l选1-OT协议类型。
     *
     * @return 2^l选1-OT协议类型。
     */
    @Override
    LotType getPtoType();

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

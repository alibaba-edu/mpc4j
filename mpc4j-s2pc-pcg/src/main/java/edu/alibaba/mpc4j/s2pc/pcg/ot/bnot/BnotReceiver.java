package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BnotFactory.BnotType;

/**
 * 基础N选1-OT协议发送方接口。
 *
 * @author Hanwen Feng
 * @date 2022/02/03
 */
public interface BnotReceiver extends TwoPartyPto, SecurePto {

    @Override
    BnotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param n      最大选择值。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int n) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择比特。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BnotReceiverOutput receive(int[] choices) throws MpcAbortException;
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.BaseNotFactory.BaseNotType;

/**
 * 基础n选1-OT协议发送方接口。
 *
 * @author Hanwen Feng
 * @date 2022/02/03
 */
public interface BaseNotReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxChoice 最大选择值。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxChoice) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择比特。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BaseNotReceiverOutput receive(int[] choices) throws MpcAbortException;
}

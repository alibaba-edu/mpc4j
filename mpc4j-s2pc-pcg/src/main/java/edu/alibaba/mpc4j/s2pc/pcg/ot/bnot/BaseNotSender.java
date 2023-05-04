package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 基础n选1-OT协议接收方接口。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public interface BaseNotSender extends TwoPartyPto {
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
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BaseNotSenderOutput send(int num) throws MpcAbortException;
}

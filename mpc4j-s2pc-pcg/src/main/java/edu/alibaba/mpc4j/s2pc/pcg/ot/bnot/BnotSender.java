package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 基础N选1-OT协议接收方接口。
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public interface BnotSender extends TwoPartyPto, SecurePto {

    @Override
    BnotFactory.BnotType getPtoType();

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
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BnotSenderOutput send(int num) throws MpcAbortException;
}

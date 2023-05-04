package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 基础OT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BaseOtSender extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BaseOtSenderOutput send(int num) throws MpcAbortException;
}

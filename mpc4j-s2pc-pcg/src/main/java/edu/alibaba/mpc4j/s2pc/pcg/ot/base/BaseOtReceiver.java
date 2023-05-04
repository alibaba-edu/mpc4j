package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory.BaseOtType;

/**
 * 基础OT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BaseOtReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择比特。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BaseOtReceiverOutput receive(boolean[] choices) throws MpcAbortException;
}

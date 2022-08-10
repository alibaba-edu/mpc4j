package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * NOT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public interface NotReceiver extends TwoPartyPto, SecurePto {

    @Override
    NotFactory.NotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param n      最大选择值。
     * @param maxNum 最大执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int n, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param choices 选择值数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    NotReceiverOutput receive(int[] choices) throws MpcAbortException;
}

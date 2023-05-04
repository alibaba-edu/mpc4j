package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;

/**
 * 核zp64乘法三元组生成协议接收方接口。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public interface Zp64CoreMtgParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num zp64三元组数量。
     * @return 参与方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Zp64Triple generate(int num) throws MpcAbortException;
}

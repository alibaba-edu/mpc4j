package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

/**
 * 核布尔三元组生成协议接口。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface Z2CoreMtgParty extends MultiPartyPto {
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
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2Triple generate(int num) throws MpcAbortException;
}

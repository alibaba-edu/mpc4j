package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 布尔三元组生成协议参与方接口。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public interface Z2MtgParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 布尔三元组数量。
     * @return 参与方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2Triple generate(int num) throws MpcAbortException;
}

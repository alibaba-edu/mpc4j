package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * l比特三元组生成协议参与方接口。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlMtgParty extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    ZlMtgFactory.ZlMtgType getPtoType();

    /**
     * 初始化协议。
     *
     * @param l           比特长度。
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int l, int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 布尔三元组数量。
     * @return 参与方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    ZlTriple generate(int num) throws MpcAbortException;
}

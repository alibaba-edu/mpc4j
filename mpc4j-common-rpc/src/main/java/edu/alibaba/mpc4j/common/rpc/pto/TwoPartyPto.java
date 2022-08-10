package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;

/**
 * 两参与方协议接口。
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface TwoPartyPto extends MultiPartyPto {
    /**
     * 返回另一个参与方信息。
     *
     * @return 另一个参与方信息。
     */
    Party otherParty();
}

package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;

/**
 * 三参与方协议接口。
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface ThreePartyPto extends MultiPartyPto {
    /**
     * 返回左参与方信息。
     *
     * @return 左参与方信息。
     */
    Party leftParty();

    /**
     * 返回右参与方信息。
     *
     * @return 右参与方信息。
     */
    Party rightParty();
}

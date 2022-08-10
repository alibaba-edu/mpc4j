package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * 抽象三方计算协议。
 *
 * @author Weiran Liu
 * @date 2022/4/29
 */
public abstract class AbstractThreePartyPto extends AbstractMultiPartyPto implements ThreePartyPto {

    /**
     * 构建三方计算协议。
     *
     * @param ptoDesc    协议描述信息。
     * @param rpc        通信接口。
     * @param leftParty  左参与方。
     * @param rightParty 右参与方。
     */
    protected AbstractThreePartyPto(PtoDesc ptoDesc, Rpc rpc, Party leftParty, Party rightParty) {
        super(ptoDesc, rpc, leftParty, rightParty);
    }

    @Override
    public Party leftParty() {
        return otherParties()[0];
    }

    @Override
    public Party rightParty() {
        return otherParties()[1];
    }
}

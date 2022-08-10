package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * 抽象两方计算协议。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public abstract class AbstractTwoPartyPto extends AbstractMultiPartyPto implements TwoPartyPto {

    /**
     * 构建两方计算协议。
     *
     * @param ptoDesc    协议描述信息。
     * @param rpc        通信接口。
     * @param otherParty 另一个参与方。
     */
    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty) {
        super(ptoDesc, rpc, otherParty);
    }

    @Override
    public Party otherParty() {
        return otherParties()[0];
    }
}

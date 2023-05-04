package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Abstract two-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public abstract class AbstractTwoPartyPto extends AbstractMultiPartyPto implements TwoPartyPto {

    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, config, rpc, otherParty);
    }

    @Override
    public Party otherParty() {
        return otherParties()[0];
    }
}

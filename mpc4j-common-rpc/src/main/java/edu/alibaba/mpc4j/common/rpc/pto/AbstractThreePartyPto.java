package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Abstract three-party protocol.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public abstract class AbstractThreePartyPto extends AbstractMultiPartyPto implements ThreePartyPto {

    protected AbstractThreePartyPto(PtoDesc ptoDesc, Rpc rpc, Party leftParty, Party rightParty, MultiPartyPtoConfig config) {
        super(ptoDesc, config, rpc, leftParty, rightParty);
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

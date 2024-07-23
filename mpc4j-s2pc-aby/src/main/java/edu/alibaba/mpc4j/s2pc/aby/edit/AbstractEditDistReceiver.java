package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Abstract edit distance receiver.
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public abstract class AbstractEditDistReceiver extends AbstractTwoPartyPto implements DistCmpReceiver {
    protected AbstractEditDistReceiver(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }
}

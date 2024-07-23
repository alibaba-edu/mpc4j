package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Abstract edit distance sender.
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public abstract class AbstractEditDistSender extends AbstractTwoPartyPto implements DistCmpSender {
    protected AbstractEditDistSender(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }
}

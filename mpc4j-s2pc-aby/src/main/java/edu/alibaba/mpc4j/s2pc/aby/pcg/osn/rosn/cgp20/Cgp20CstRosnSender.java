package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.cgp20;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractCstRosnSender;

/**
 * CGP20 CST Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public class Cgp20CstRosnSender extends AbstractCstRosnSender {

    public Cgp20CstRosnSender(Rpc senderRpc, Party receiverParty, Cgp20CstRosnConfig config) {
        super(Cgp20CstRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }
}

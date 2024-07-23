package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.lll24;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractCstRosnReceiver;

/**
 * LLL24 CST Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/9
 */
public class Lll24CstRosnReceiver extends AbstractCstRosnReceiver {

    public Lll24CstRosnReceiver(Rpc senderRpc, Party receiverParty, Lll24CstRosnConfig config) {
        super(Lll24CstRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
    }
}

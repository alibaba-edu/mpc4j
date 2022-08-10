package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * MPOPRF发送方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public abstract class AbstractMpOprfSender extends AbstractOprfSender implements MpOprfSender {

    protected AbstractMpOprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, MpOprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }
}

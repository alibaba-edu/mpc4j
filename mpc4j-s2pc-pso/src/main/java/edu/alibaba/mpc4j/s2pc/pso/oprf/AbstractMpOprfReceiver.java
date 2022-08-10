package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * MPOPRF接收方。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public abstract class AbstractMpOprfReceiver extends AbstractOprfReceiver implements MpOprfReceiver {

    protected AbstractMpOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, MpOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }
}

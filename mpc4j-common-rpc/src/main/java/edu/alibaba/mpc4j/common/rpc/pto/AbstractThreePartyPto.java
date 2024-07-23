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
    /**
     * Creates a three party protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param leftParty  left party.
     * @param rightParty right party.
     * @param config     config.
     */
    protected AbstractThreePartyPto(PtoDesc ptoDesc, Rpc ownRpc, Party leftParty, Party rightParty,
                                    MultiPartyPtoConfig config) {
        super(ptoDesc, config, ownRpc, leftParty, rightParty);
    }
}

package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractBopprfScpsiClient;

/**
 * RS21 server-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public class Rs21ScpsiClient<T> extends AbstractBopprfScpsiClient<T> {

    public Rs21ScpsiClient(Rpc clientRpc, Party senderParty, Rs21ScpsiConfig config) {
        super(Rs21ScpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}

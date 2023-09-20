package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractBopprfScpsiClient;

/**
 * PSTY19 server-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Psty19ScpsiClient<T> extends AbstractBopprfScpsiClient<T> {

    public Psty19ScpsiClient(Rpc clientRpc, Party senderParty, Psty19ScpsiConfig config) {
        super(Psty19ScpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}

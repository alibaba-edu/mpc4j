package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.AbstractBopprfCcpsiClient;

/**
 * PSTY19 client-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Psty19CcpsiClient<T> extends AbstractBopprfCcpsiClient<T> {

    public Psty19CcpsiClient(Rpc serverRpc, Party clientParty, Psty19CcpsiConfig config) {
        super(Psty19CcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.AbstractBopprfCcpsiClient;

/**
 * RS21 client-payload circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/7/28
 */
public class Rs21CcpsiClient<T> extends AbstractBopprfCcpsiClient<T> {

    public Rs21CcpsiClient(Rpc serverRpc, Party clientParty, Rs21CcpsiConfig config) {
        super(Rs21CcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

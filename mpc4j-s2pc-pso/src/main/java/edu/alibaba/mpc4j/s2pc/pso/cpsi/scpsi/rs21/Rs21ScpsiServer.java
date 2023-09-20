package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractBopprfScpsiServer;

/**
 * RS21 server-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public class Rs21ScpsiServer<T> extends AbstractBopprfScpsiServer<T> {

    public Rs21ScpsiServer(Rpc serverRpc, Party clientParty, Rs21ScpsiConfig config) {
        super(Rs21ScpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

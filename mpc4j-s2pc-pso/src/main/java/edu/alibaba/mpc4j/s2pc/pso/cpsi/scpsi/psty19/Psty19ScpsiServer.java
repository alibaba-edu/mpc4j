package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractBopprfScpsiServer;

/**
 * PSTY19 server-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Psty19ScpsiServer<T> extends AbstractBopprfScpsiServer<T> {

    public Psty19ScpsiServer(Rpc serverRpc, Party clientParty, Psty19ScpsiConfig config) {
        super(Psty19ScpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

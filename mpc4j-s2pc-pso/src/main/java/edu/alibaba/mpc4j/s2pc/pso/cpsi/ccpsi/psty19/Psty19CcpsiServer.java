package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.AbstractBopprfCcpsiServer;

/**
 * PSTY19 client-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Psty19CcpsiServer<T> extends AbstractBopprfCcpsiServer<T> {

    public Psty19CcpsiServer(Rpc clientRpc, Party senderParty, Psty19CcpsiConfig config) {
        super(Psty19CcpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}

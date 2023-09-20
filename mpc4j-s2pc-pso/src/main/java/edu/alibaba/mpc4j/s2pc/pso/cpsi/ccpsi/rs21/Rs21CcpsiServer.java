package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.AbstractBopprfCcpsiServer;

/**
 * RS21 client-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/7/28
 */
public class Rs21CcpsiServer<T> extends AbstractBopprfCcpsiServer<T> {

    public Rs21CcpsiServer(Rpc clientRpc, Party senderParty, Rs21CcpsiConfig config) {
        super(Rs21CcpsiPtoDesc.getInstance(), clientRpc, senderParty, config);
    }
}

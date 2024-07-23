package edu.alibaba.mpc4j.s2pc.pso.psi.aid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;

/**
 * abstract aid PSI server.
 *
 * @author Weiran Liu
 * @date 2024/6/10
 */
public abstract class AbstractAidPsiServer<T> extends AbstractPsiServer<T> {

    protected AbstractAidPsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, Party aiderParty, AidPsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, aiderParty, config);
    }
}

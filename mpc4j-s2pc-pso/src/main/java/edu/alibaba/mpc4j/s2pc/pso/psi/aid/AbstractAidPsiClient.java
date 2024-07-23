package edu.alibaba.mpc4j.s2pc.pso.psi.aid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;

/**
 * abstract aid PSI client.
 *
 * @author Weiran Liu
 * @date 2024/6/10
 */
public abstract class AbstractAidPsiClient<T> extends AbstractPsiClient<T> {

    protected AbstractAidPsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, Party aiderParty, AidPsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, aiderParty, config);
    }
}

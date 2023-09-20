package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiServer;

/**
 * RR22-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rr22PsiServer<T> extends AbstractMpOprfPsiServer<T> {

    public Rr22PsiServer(Rpc serverRpc, Party clientParty, Rr22PsiConfig config) {
        super(Rr22PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

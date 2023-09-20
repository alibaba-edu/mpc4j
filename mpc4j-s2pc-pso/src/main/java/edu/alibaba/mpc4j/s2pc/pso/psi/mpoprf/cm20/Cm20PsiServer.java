package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiServer;

/**
 * CM20-PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/10
 */
public class Cm20PsiServer<T> extends AbstractMpOprfPsiServer<T> {

    public Cm20PsiServer(Rpc serverRpc, Party clientParty, Cm20PsiConfig config) {
        super(Cm20PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

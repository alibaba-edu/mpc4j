package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiClient;

/**
 * CM20-PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/10
 */
public class Cm20PsiClient<T> extends AbstractMpOprfPsiClient<T> {

    public Cm20PsiClient(Rpc clientRpc, Party serverParty, Cm20PsiConfig config) {
        super(Cm20PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}

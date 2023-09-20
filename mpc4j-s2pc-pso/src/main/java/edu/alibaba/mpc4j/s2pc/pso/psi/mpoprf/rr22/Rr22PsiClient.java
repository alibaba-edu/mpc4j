package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiClient;

/**
 * RR22-PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rr22PsiClient<T> extends AbstractMpOprfPsiClient<T> {

    public Rr22PsiClient(Rpc clientRpc, Party serverParty, Rr22PsiConfig config) {
        super(Rr22PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}

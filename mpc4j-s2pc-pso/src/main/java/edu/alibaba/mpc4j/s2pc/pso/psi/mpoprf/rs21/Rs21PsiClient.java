package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiClient;

/**
 * RS21-PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rs21PsiClient<T> extends AbstractMpOprfPsiClient<T> {

    public Rs21PsiClient(Rpc clientRpc, Party serverParty, Rs21PsiConfig config) {
        super(Rs21PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}

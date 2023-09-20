package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rs21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.AbstractMpOprfPsiServer;

/**
 * RS21-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Rs21PsiServer<T> extends AbstractMpOprfPsiServer<T> {

    public Rs21PsiServer(Rpc serverRpc, Party clientParty, Rs21PsiConfig config) {
        super(Rs21PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

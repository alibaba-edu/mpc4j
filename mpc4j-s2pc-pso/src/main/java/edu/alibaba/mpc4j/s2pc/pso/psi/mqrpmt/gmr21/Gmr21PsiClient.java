package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.AbstractMqRpmtPsiClient;

/**
 * GMR21-PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Gmr21PsiClient<T> extends AbstractMqRpmtPsiClient<T> {

    public Gmr21PsiClient(Rpc clientRpc, Party serverParty, Gmr21PsiConfig config) {
        super(Gmr21PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}

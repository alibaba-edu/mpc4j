package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.AbstractMqRpmtPsiClient;

/**
 * CZZ22-PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiClient<T> extends AbstractMqRpmtPsiClient<T> {

    public Czz22PsiClient(Rpc clientRpc, Party serverParty, Czz22PsiConfig config) {
        super(Czz22PsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}

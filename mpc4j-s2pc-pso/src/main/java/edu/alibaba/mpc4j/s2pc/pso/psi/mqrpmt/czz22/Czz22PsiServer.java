package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.AbstractMqRpmtPsiServer;

/**
 * CZZ22-PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiServer<T> extends AbstractMqRpmtPsiServer<T> {

    public Czz22PsiServer(Rpc serverRpc, Party clientParty, Czz22PsiConfig config) {
        super(Czz22PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.AbstractMqRpmtPsiServer;

/**
 * GMR21-PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Gmr21PsiServer<T> extends AbstractMqRpmtPsiServer<T> {

    public Gmr21PsiServer(Rpc serverRpc, Party clientParty, Gmr21PsiConfig config) {
        super(Gmr21PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}
package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.AbstractSqOprfPsiServer;

/**
 * RA17-ECC-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Ra17EccPsiServer<T> extends AbstractSqOprfPsiServer<T> {

    public Ra17EccPsiServer(Rpc serverRpc, Party clientParty, Ra17EccPsiConfig config) {
        super(Ra17EccPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}


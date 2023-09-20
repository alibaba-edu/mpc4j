package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.AbstractSqOprfPsiClient;

/**
 * RA17-ECC-PSI client.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Ra17EccPsiClient<T> extends AbstractSqOprfPsiClient<T> {

    public Ra17EccPsiClient(Rpc clientRpc, Party serverParty, Ra17EccPsiConfig config) {
        super(Ra17EccPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
    }
}


package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.AbstractSqOprfPsiServer;

/**
 * RA17-BYTE-ECC-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Ra17ByteEccPsiServer<T> extends AbstractSqOprfPsiServer<T> {

    public Ra17ByteEccPsiServer(Rpc serverRpc, Party clientParty, Ra17ByteEccPsiConfig config) {
        super(Ra17ByteEccPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
    }
}

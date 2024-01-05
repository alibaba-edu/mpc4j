package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;

/**
 * abstract single client-specific preprocessing PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractSingleCpPirServer extends AbstractTwoPartyPto implements SingleCpPirServer {
    /**
     * database size
     */
    protected int n;
    /**
     * value bit length
     */
    protected int l;
    /**
     * value byte length
     */
    protected int byteL;

    protected AbstractSingleCpPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty,
                                        SingleCpPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(ZlDatabase database) {
        n = database.rows();
        l = database.getL();
        byteL = database.getByteL();
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}

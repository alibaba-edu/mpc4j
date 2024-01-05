package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;

/**
 * Abstract Single Index PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractSingleIndexPirServer extends AbstractTwoPartyPto implements SingleIndexPirServer {
    /**
     * partition database
     */
    protected ZlDatabase[] databases;
    /**
     * database size
     */
    protected int num;
    /**
     * partition size
     */
    protected int partitionSize;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition byte length
     */
    protected int partitionByteLength;

    protected AbstractSingleIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty,
                                           SingleIndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(NaiveDatabase database) {
        num = database.rows();
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
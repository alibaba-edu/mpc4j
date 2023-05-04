package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;

/**
 * Abstract Index PIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public abstract class AbstractIndexPirServer extends AbstractTwoPartyPto implements IndexPirServer {
    /**
     * partition database
     */
    protected ZlDatabase[] databases;
    /**
     * database size
     */
    protected int num;

    protected AbstractIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, IndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(NaiveDatabase database, int partitionByteLength) {
        num = database.rows();
        databases = database.partitionZl(partitionByteLength * Byte.SIZE);
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
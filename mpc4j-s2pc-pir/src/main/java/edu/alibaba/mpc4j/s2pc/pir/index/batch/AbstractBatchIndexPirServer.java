package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;

import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchIndexPirServer extends AbstractTwoPartyPto implements BatchIndexPirServer {
    /**
     * partition database
     */
    protected ZlDatabase[] databases;
    /**
     * database size
     */
    protected int num;
    /**
     * element bit length
     */
    protected int elementBitLength;
    /**
     * max retrieval size
     */
    protected int maxRetrievalSize;
    /**
     * partition size
     */
    protected int partitionSize;
    /**
     * element byte length
     */
    protected int elementByteLength;

    protected AbstractBatchIndexPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(NaiveDatabase database, int maxRetrievalSize) {
        num = database.rows();
        elementBitLength = database.getL();
        elementByteLength = database.getByteL();
        this.maxRetrievalSize = maxRetrievalSize;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
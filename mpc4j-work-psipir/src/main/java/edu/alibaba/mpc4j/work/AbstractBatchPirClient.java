package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.List;

/**
 * abstract batch PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchPirClient extends AbstractTwoPartyPto implements BatchPirClient {
    /**
     * max client retrieval size
     */
    protected int maxRetrievalSize;
    /**
     * client retrieval size
     */
    protected int retrievalSize;
    /**
     * index list
     */
    protected List<Integer> indexList;
    /**
     * server element size
     */
    protected int serverElementSize;

    protected AbstractBatchPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, BatchPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int serverElementSize, int maxRetrievalSize) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        this.serverElementSize = serverElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        initState();
    }

    protected void setPtoInput(List<Integer> indexList) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("maxRetrievalSize", indexList.size(), maxRetrievalSize);
        for (Integer index : indexList) {
            MathPreconditions.checkNonNegativeInRange("index", index, serverElementSize);
        }
        this.retrievalSize = indexList.size();
        this.indexList = indexList;
        extraInfo++;
    }
}
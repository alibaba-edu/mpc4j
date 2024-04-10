package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.List;

/**
 * abstract batch Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchIndexPirClient extends AbstractTwoPartyPto implements BatchIndexPirClient {
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
    /**
     * element bit length
     */
    protected int elementBitLength;
    /**
     * element byte length
     */
    protected int elementByteLength;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition size
     */
    protected int partitionSize;

    protected AbstractBatchIndexPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, BatchIndexPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int serverElementSize, int elementBitLength, int maxRetrievalSize) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        this.serverElementSize = serverElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        this.elementBitLength = elementBitLength;
        this.elementByteLength = CommonUtils.getByteLength(elementBitLength);
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
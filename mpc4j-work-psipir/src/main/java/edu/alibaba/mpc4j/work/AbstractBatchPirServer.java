package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * abstract batch PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public abstract class AbstractBatchPirServer extends AbstractTwoPartyPto implements BatchPirServer {
    /**
     * database
     */
    protected List<byte[]> databaseList;
    /**
     * database size
     */
    protected int num;
    /**
     * max retrieval size
     */
    protected int maxRetrievalSize;

    protected AbstractBatchPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, BatchPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(BitVector database, int maxRetrievalSize) {
        MathPreconditions.checkPositive("serverElementSize", database.bitNum());
        num = database.bitNum();
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        this.databaseList = new ArrayList<>();
        for (int j = 0; j < num; j++) {
            boolean value = database.get(j);
            if (value) {
                databaseList.add(IntUtils.intToByteArray(j));
            }
        }
        int paddingNum = num - databaseList.size();
        for (int j = 0; j < paddingNum; j++) {
            databaseList.add(IntUtils.intToByteArray(num + j));
        }
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
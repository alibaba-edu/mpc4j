package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract standard index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public abstract class AbstractStdIdxPirServer extends AbstractTwoPartyPto implements StdIdxPirServer {
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
    /**
     * mat batch num
     */
    private int maxBatchNum;

    protected AbstractStdIdxPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, StdIdxPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(NaiveDatabase database, int maxBatchNum) {
        n = database.rows();
        l = database.getL();
        byteL = database.getByteL();
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void checkInitInput(NaiveDatabase database, int maxBatchNum) {
        n = database.rows();
        l = database.getL();
        byteL = database.getByteL();
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
    }

    protected void setPtoInput(int batchNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", batchNum, maxBatchNum);
    }
}

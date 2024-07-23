package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractCpIdxPirServer extends AbstractTwoPartyPto implements CpIdxPirServer {
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

    protected AbstractCpIdxPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, CpIdxPirConfig config) {
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

    protected void setPtoInput(int batchNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", batchNum, maxBatchNum);
    }
}

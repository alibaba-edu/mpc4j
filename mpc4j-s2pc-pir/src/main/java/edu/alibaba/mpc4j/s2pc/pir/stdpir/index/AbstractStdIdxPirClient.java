package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;

import java.util.Arrays;

/**
 * abstract standard index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public abstract class AbstractStdIdxPirClient extends AbstractTwoPartyPto implements StdIdxPirClient {
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
    protected int maxBatchNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractStdIdxPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, StdIdxPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void checkInitInput(int n, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositiveInRangeClosed("max_batch_num", maxBatchNum, PirUtils.MAX_BATCH_NUM);
        this.maxBatchNum = maxBatchNum;
    }

    protected void setPtoInput(int[] xs) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", xs.length, maxBatchNum);
        batchNum = xs.length;
        Arrays.stream(xs).forEach(x -> MathPreconditions.checkNonNegativeInRange("x", x, n));
    }
}

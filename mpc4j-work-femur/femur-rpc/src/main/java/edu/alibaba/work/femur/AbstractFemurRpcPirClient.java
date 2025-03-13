package edu.alibaba.work.femur;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * Abstract PGM-index range keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public abstract class AbstractFemurRpcPirClient extends AbstractTwoPartyPto implements FemurRpcPirClient {
    /**
     * database size
     */
    protected int n;
    /**
     * value bit length
     */
    protected int l;
    /**
     * long for ⊥
     */
    protected long botLong;
    /**
     * longL
     */
    protected int longL;
    /**
     * byteL
     */
    protected int byteL;
    /**
     * max batch num
     */
    protected int maxBatchNum;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * range bound
     */
    protected int rangeBound;
    /**
     * epsilon
     */
    protected double epsilon;

    protected AbstractFemurRpcPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, FemurRpcPirConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int n, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        MathPreconditions.checkEqual("l", "0", l % Long.SIZE, 0);
        this.l = l;
        this.longL = CommonUtils.getLongLength(l);
        this.byteL = CommonUtils.getByteLength(l);
        botLong = Long.MAX_VALUE;
        MathPreconditions.checkPositive("max batch num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(long[] keys, int rangeBound, double epsilon) {
        MathPreconditions.checkPositiveInRangeClosed("batch num", keys.length, maxBatchNum);
        batchNum = keys.length;
        Arrays.stream(keys).forEach(key -> Preconditions.checkArgument(key != botLong, "x must not equal ⊥"));
        MathPreconditions.checkPositive("range bound", rangeBound);
        this.rangeBound = rangeBound;
        MathPreconditions.checkPositive("epsilon", epsilon);
        this.epsilon = epsilon;
        checkInitialized();
    }
}

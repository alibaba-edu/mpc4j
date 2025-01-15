package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * abstract client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractCpIdxPirClient extends AbstractTwoPartyPto implements CpIdxPirClient {
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

    protected AbstractCpIdxPirClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, CpIdxPirConfig config) {
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

    protected void setPtoInput(int[] xs) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", xs.length, maxBatchNum);
        batchNum = xs.length;
        Arrays.stream(xs).forEach(x -> MathPreconditions.checkNonNegativeInRange("x", x, n));
    }
}

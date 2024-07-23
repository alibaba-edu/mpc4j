package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.stream.IntStream;

/**
 * Abstract Zl lookup table protocol receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/3
 */
public abstract class AbstractZlLutReceiver extends AbstractTwoPartyPto implements ZlLutReceiver {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max input l
     */
    protected int maxM;
    /**
     * max output l
     */
    protected int maxN;
    /**
     * num
     */
    protected int num;
    /**
     * input l
     */
    protected int m;
    /**
     * output l
     */
    protected int n;
    /**
     * input l in bytes
     */
    protected int byteM;
    /**
     * output l in bytes
     */
    protected int byteN;

    protected AbstractZlLutReceiver(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxM, int maxN, int maxNum) {
        MathPreconditions.checkPositive("maxM", maxM);
        this.maxM = maxM;
        MathPreconditions.checkPositive("maxN", maxN);
        this.maxN = maxN;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(byte[][] inputs, int m, int n) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("m", m, maxM);
        this.m = m;
        byteM = CommonUtils.getByteLength(m);
        MathPreconditions.checkPositiveInRangeClosed("n", n, maxN);
        this.n = n;
        byteN = CommonUtils.getByteLength(n);
        MathPreconditions.checkPositiveInRangeClosed("inputs.num", inputs.length, maxNum);
        num = inputs.length;
        IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.isFixedReduceByteArray(inputs[i], byteM, m))
            .forEach(Preconditions::checkArgument);
    }
}

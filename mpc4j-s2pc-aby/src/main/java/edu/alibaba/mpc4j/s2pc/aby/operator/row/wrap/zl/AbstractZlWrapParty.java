package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * Abstract Zl wrap protocol party.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public abstract class AbstractZlWrapParty extends AbstractTwoPartyPto implements ZlWrapParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max l
     */
    protected int maxL;
    /**
     * num
     */
    protected int num;
    /**
     * l
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * inputs
     */
    protected byte[][] inputs;

    protected AbstractZlWrapParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][] inputs) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", l, maxL);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositiveInRangeClosed("inputs.num", inputs.length, maxNum);
        num = inputs.length;
        this.inputs = Arrays.stream(inputs)
                .peek(input -> Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(input, byteL, l)))
                .toArray(byte[][]::new);
    }
}

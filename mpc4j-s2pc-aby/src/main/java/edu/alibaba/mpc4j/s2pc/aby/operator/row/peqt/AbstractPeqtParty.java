package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * abstract private equality test party.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public abstract class AbstractPeqtParty extends AbstractTwoPartyPto implements PeqtParty {
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

    public AbstractPeqtParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, PeqtConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int l, byte[][] inputs) {
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

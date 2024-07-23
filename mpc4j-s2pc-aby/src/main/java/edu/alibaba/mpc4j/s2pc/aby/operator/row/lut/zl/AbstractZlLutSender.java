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

/**
 * Abstract Zl lookup table protocol sender.
 *
 * @author Liqiang Peng
 * @date 2024/5/31
 */
public abstract class AbstractZlLutSender extends AbstractTwoPartyPto implements ZlLutSender {
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

    protected AbstractZlLutSender(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
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

    protected void setPtoInput(byte[][][] table, int m, int n) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("m", m, maxM);
        this.m = m;
        byteM = CommonUtils.getByteLength(m);
        MathPreconditions.checkPositiveInRangeClosed("n", n, maxN);
        this.n = n;
        byteN = CommonUtils.getByteLength(n);
        MathPreconditions.checkPositiveInRangeClosed("table.num", table.length, maxNum);
        num = table.length;
        for (int i = 0; i < num; i++) {
            MathPreconditions.checkEqual("table entries num", "m", table[i].length, 1 << m);
            for (int j = 0; j < 1 << m; j++) {
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(table[i][j], byteN, n));
            }
        }
    }
}

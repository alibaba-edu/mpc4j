package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * 汉明距离协议参与方。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public abstract class AbstractHammingParty extends AbstractTwoPartyPto implements HammingParty {
    /**
     * 最大比特数量
     */
    protected int maxBitNum;
    /**
     * 当前比特数量
     */
    protected int bitNum;

    public AbstractHammingParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, HammingConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        maxBitNum = 0;
        bitNum = 0;
    }

    protected void setInitInput(int maxBitNum) {
        MathPreconditions.checkPositive("maxBitNum", maxBitNum);
        this.maxBitNum = maxBitNum;
        initState();
    }

    protected void setPtoInput(SquareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxBitNum);
        bitNum = xi.getNum();
        extraInfo++;
    }
}

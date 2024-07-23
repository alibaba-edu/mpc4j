package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * abstract Zl64 triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public abstract class AbstractZl64TripleGenParty extends AbstractTwoPartyPto implements Zl64TripleGenParty {
    /**
     * config
     */
    protected final Zl64TripleGenConfig config;
    /**
     * maxL
     */
    protected int maxL;
    /**
     * Zl
     */
    protected Zl64 zl64;
    /**
     * l
     */
    protected int l;
    /**
     * byte l
     */
    protected int byteL;
    /**
     * num
     */
    protected int num;

    public AbstractZl64TripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Zl64TripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    public AbstractZl64TripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Party aiderParty,
                                    Zl64TripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, aiderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxL", maxL, Long.SIZE);
        MathPreconditions.checkPositive("expect_num", expectTotalNum);
        this.maxL = maxL;
        initState();
    }

    @Override
    public void init(int maxL) throws MpcAbortException {
        init(maxL, config.defaultRoundNum(maxL));
    }

    protected void setPtoInput(Zl64 zl64, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        MathPreconditions.checkPositive("num", num);
        this.zl64 = zl64;
        l = zl64.getL();
        byteL = zl64.getByteL();
        this.num = num;
    }
}

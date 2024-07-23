package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;

/**
 * abstract Zl triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public abstract class AbstractZlTripleGenParty extends AbstractTwoPartyPto implements ZlTripleGenParty {
    /**
     * config
     */
    protected final ZlTripleGenConfig config;
    /**
     * maxL
     */
    protected int maxL;
    /**
     * Zl
     */
    protected Zl zl;
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

    public AbstractZlTripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlTripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    public AbstractZlTripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Party aiderParty,
                                    ZlTripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, aiderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        MathPreconditions.checkPositive("expect_num", expectTotalNum);
        this.maxL = maxL;
        initState();
    }

    @Override
    public void init(int maxL) throws MpcAbortException {
        init(maxL, config.defaultRoundNum(maxL));
    }

    protected void setPtoInput(Zl zl, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        this.zl = zl;
        l = zl.getL();
        byteL = zl.getByteL();
        this.num = num;
    }
}

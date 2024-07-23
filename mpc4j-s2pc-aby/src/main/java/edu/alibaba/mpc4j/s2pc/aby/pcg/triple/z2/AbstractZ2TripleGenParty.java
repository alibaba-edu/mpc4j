package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract Z2 triple generation party.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public abstract class AbstractZ2TripleGenParty extends AbstractTwoPartyPto implements Z2TripleGenParty {
    /**
     * config
     */
    protected final Z2TripleGenConfig config;
    /**
     * expect num
     */
    protected int expectNum;
    /**
     * num
     */
    protected int num;
    /**
     * num in bytes
     */
    protected int byteNum;

    protected AbstractZ2TripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2TripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected AbstractZ2TripleGenParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Party aiderParty,
                                       Z2TripleGenConfig config) {
        super(ptoDesc, ownRpc, otherParty, aiderParty, config);
        this.config = config;
    }

    protected void setInitInput(int expectNum) {
        MathPreconditions.checkPositive("expect_num", expectNum);
        this.expectNum = expectNum;
        initState();
    }

    @Override
    public void init() throws MpcAbortException {
        init(config.defaultRoundNum());
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}

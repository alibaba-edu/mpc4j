package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * abstract GF2K-MSP-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kMspVoleSender extends AbstractTwoPartyPto implements Gf2kMspVoleSender {
    /**
     * config
     */
    private final Gf2kMspVoleConfig config;
    /**
     * field
     */
    protected Sgf2k field;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * num
     */
    protected int num;
    /**
     * sparse num
     */
    protected int t;

    protected AbstractGf2kMspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kMspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
        this.subfieldL = subfieldL;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        this.t = t;
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preSenderOutput.getNum(), Gf2kMspVoleFactory.getPrecomputeNum(config, subfieldL, t, num)
            );
        }
    }
}

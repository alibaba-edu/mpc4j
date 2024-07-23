package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * abstract GF2K-MSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public abstract class AbstractGf2kMspVodeSender extends AbstractTwoPartyPto implements Gf2kMspVodeSender {
    /**
     * config
     */
    private final Gf2kMspVodeConfig config;
    /**
     * field
     */
    protected Dgf2k field;
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

    protected AbstractGf2kMspVodeSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kMspVodeConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        this.subfieldL = subfieldL;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        this.t = t;
    }

    protected void setPtoInput(int t, int num, Gf2kVodeSenderOutput preSenderOutput) {
        setPtoInput(t, num);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preSenderOutput.getNum(), Gf2kMspVodeFactory.getPrecomputeNum(config, subfieldL, t, num)
            );
        }
    }
}

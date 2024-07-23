package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

/**
 * abstract GF2K-MSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public abstract class AbstractGf2kMspVodeReceiver extends AbstractTwoPartyPto implements Gf2kMspVodeReceiver {
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
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;
    /**
     * sparse num
     */
    protected int t;

    protected AbstractGf2kMspVodeReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kMspVodeConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        this.subfieldL = subfieldL;
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = delta;
        initState();
    }

    protected void setPtoInput(int t, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        MathPreconditions.checkPositiveInRangeClosed("t", t, num);
        this.t = t;
    }

    protected void setPtoInput(int t, int num, Gf2kVodeReceiverOutput preReceiverOutput) {
        setPtoInput(t, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(), Gf2kMspVodeFactory.getPrecomputeNum(config, subfieldL, t, num)
            );
        }
    }
}

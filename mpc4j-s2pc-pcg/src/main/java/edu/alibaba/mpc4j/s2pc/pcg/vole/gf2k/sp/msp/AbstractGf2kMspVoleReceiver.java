package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * abstract GF2K-MSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kMspVoleReceiver extends AbstractTwoPartyPto implements Gf2kMspVoleReceiver {
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

    protected AbstractGf2kMspVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kMspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
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
        extraInfo++;
    }

    protected void setPtoInput(int t, int num, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(t, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(), Gf2kMspVoleFactory.getPrecomputeNum(config, subfieldL, t, num)
            );
        }
    }
}

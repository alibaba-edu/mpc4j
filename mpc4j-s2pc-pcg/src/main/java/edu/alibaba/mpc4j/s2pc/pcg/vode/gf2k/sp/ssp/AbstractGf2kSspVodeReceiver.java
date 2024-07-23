package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeReceiverOutput;

import java.util.Arrays;

/**
 * GF2K-SSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public abstract class AbstractGf2kSspVodeReceiver extends AbstractTwoPartyPto implements Gf2kSspVodeReceiver {
    /**
     * config
     */
    protected final Gf2kSspVodeConfig config;
    /**
     * field
     */
    protected Dgf2k field;
    /**
     * field byte L
     */
    protected int fieldByteL;
    /**
     * field L
     */
    protected int fieldL;
    /**
     * subfield
     */
    protected Gf2e subfield;
    /**
     * subfield L
     */
    protected int subfieldL;
    /**
     * subfield Byte L
     */
    protected int subfieldByteL;
    /**
     * r
     */
    protected int r;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kSspVodeReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kSspVodeConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = delta;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
    }

    protected void setPtoInput(int num, Gf2kVodeReceiverOutput preReceiverOutput) {
        setPtoInput(num);
        if (preReceiverOutput != null) {
            Preconditions.checkArgument(Arrays.equals(delta, preReceiverOutput.getDelta()));
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(), Gf2kSspVodeFactory.getPrecomputeNum(config, subfieldL, num)
            );
        }
    }
}

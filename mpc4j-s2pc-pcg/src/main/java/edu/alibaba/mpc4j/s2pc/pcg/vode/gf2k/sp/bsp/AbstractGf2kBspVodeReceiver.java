package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

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
 * abstract GF2K-BSP-VODE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public abstract class AbstractGf2kBspVodeReceiver extends AbstractTwoPartyPto implements Gf2kBspVodeReceiver {
    /**
     * config
     */
    protected final Gf2kBspVodeConfig config;
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
     * num for each GF2K-SSP-VOLE
     */
    protected int eachNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractGf2kBspVodeReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kBspVodeConfig config) {
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

    protected void setPtoInput(int batchNum, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositive("each_num", eachNum);
        this.eachNum = eachNum;
        MathPreconditions.checkPositive("batch_num", batchNum);
        this.batchNum = batchNum;
    }

    protected void setPtoInput(int batchNum, int eachNum, Gf2kVodeReceiverOutput preReceiverOutput) {
        setPtoInput(batchNum, eachNum);
        if (preReceiverOutput != null) {
            Preconditions.checkArgument(Arrays.equals(delta, preReceiverOutput.getDelta()));
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(),
                Gf2kBspVodeFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum)
            );
        }
    }
}

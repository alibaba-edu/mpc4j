package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

import java.util.Arrays;

/**
 * abstract GF2K-BSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public abstract class AbstractGf2kBspVoleReceiver extends AbstractTwoPartyPto implements Gf2kBspVoleReceiver {
    /**
     * config
     */
    protected final Gf2kBspVoleConfig config;
    /**
     * field
     */
    protected Sgf2k field;
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

    protected AbstractGf2kBspVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kBspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL, byte[] delta) {
        field = Sgf2kFactory.getInstance(envType, subfieldL);
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
        extraInfo++;
    }

    protected void setPtoInput(int batchNum, int eachNum, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(batchNum, eachNum);
        if (preReceiverOutput != null) {
            Preconditions.checkArgument(Arrays.equals(delta, preReceiverOutput.getDelta()));
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preReceiverOutput.getNum(),
                Gf2kBspVoleFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum)
            );
        }
    }
}

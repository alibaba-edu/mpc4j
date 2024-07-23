package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Dgf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

import java.util.Arrays;

/**
 * abstract GF2K-BSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public abstract class AbstractGf2kBspVodeSender extends AbstractTwoPartyPto implements Gf2kBspVodeSender {
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
     * α array
     */
    protected int[] alphaArray;
    /**
     * num for each GF2K-SSP-VOLE
     */
    protected int eachNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractGf2kBspVodeSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kBspVodeConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput(int subfieldL) {
        field = Dgf2kFactory.getInstance(envType, subfieldL);
        fieldL = field.getL();
        fieldByteL = field.getByteL();
        subfield = field.getSubfield();
        this.subfieldL = subfield.getL();
        subfieldByteL = subfield.getByteL();
        r = field.getR();
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositive("each_num", eachNum);
        this.eachNum = eachNum;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositive("batch_num", batchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, eachNum))
            .toArray();
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, Gf2kVodeSenderOutput preSenderOutput) {
        setPtoInput(alphaArray, eachNum);
        if (preSenderOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preNum", preSenderOutput.getNum(),
                Gf2kBspVodeFactory.getPrecomputeNum(config, subfieldL, batchNum, eachNum)
            );
        }
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

import java.util.Arrays;

/**
 * abstract GF2K-BSP-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public abstract class AbstractGf2kBspVoleSender extends AbstractTwoPartyPto implements Gf2kBspVoleSender {
    /**
     * config
     */
    protected final Gf2kBspVoleConfig config;
    /**
     * GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * max num for each GF2K-SSP-VOLE
     */
    private int maxEachNum;
    /**
     * max batch num
     */
    private int maxBatchNum;
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

    protected AbstractGf2kBspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Gf2kBspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
        gf2k = Gf2kFactory.createInstance(envType);
    }

    protected void setInitInput(int maxBatchNum, int maxEachNum) {
        MathPreconditions.checkPositive("maxEachNum", maxEachNum);
        this.maxEachNum = maxEachNum;
        MathPreconditions.checkPositive("maxBatchNum", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", eachNum, maxEachNum);
        this.eachNum = eachNum;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, eachNum))
            .toArray();
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, Gf2kVoleSenderOutput preSenderOutput) {
        setPtoInput(alphaArray, eachNum);
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preSenderOutput.getNum(), Gf2kBspVoleFactory.getPrecomputeNum(config, batchNum, eachNum)
        );
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract BSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotReceiver extends AbstractTwoPartyPto implements BspCotReceiver {
    /**
     * config
     */
    private final BspCotConfig config;
    /**
     * max num for each SSP-COT
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
     * num for each SSP-COT
     */
    protected int eachNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractBspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
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
        MathPreconditions.checkPositiveInRangeClosed("eachNum", eachNum, maxEachNum);
        this.eachNum = eachNum;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositiveInRangeClosed("batchNum", batchNum, maxBatchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, eachNum))
            .toArray();
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, eachNum);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preReceiverOutput.getNum(), BspCotFactory.getPrecomputeNum(config, batchNum, eachNum)
        );
    }
}

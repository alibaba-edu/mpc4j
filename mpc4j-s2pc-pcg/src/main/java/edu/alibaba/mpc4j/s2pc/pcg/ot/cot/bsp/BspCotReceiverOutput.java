package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotReceiverOutput;

import java.util.Arrays;

/**
 * Batched single-point COT receiver output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotReceiverOutput implements BatchPcgOutput {
    /**
     * SSP-COT receiver outputs
     */
    private SspCotReceiverOutput[] receiverOutputs;
    /**
     * num for each SSP-COT receiver output
     */
    private int eachNum;

    public static BspCotReceiverOutput create(SspCotReceiverOutput[] sspCotReceiverOutputs) {
        BspCotReceiverOutput receiverOutput = new BspCotReceiverOutput();
        MathPreconditions.checkPositive("num", sspCotReceiverOutputs.length);
        // get each num
        receiverOutput.eachNum = sspCotReceiverOutputs[0].getNum();
        // set other outputs
        receiverOutput.receiverOutputs = Arrays.stream(sspCotReceiverOutputs)
            // 验证数量均为num
            .peek(iOutput ->
                MathPreconditions.checkEqual("each num", "i-th num", receiverOutput.eachNum, iOutput.getNum())
            )
            .toArray(SspCotReceiverOutput[]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private BspCotReceiverOutput() {
        // empty
    }

    /**
     * Gets the assigned SSP-COT receiver output.
     *
     * @param index index.
     * @return the assigned SSP-COT receiver output.
     */
    public SspCotReceiverOutput get(int index) {
        return receiverOutputs[index];
    }

    /**
     * Gets num for each SSP-COT receiver output.
     *
     * @return num for each SSP-COT receiver output.
     */
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getBatchNum() {
        return receiverOutputs.length;
    }
}

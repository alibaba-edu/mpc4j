package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.BatchPcgOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.ssp.SspCotSenderOutput;

/**
 * Batched single-point COT sender output.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotSenderOutput implements BatchPcgOutput {
    /**
     * SSP-COT sender outputs
     */
    private SspCotSenderOutput[] senderOutputs;
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * num in each SSP-COT sender output
     */
    private int eachNum;

    /**
     * Creates a sender output.
     *
     * @param sspCotSenderOutputs SSP-COT sender outputs.
     * @return a sender output.
     */
    public static BspCotSenderOutput create(SspCotSenderOutput[] sspCotSenderOutputs) {
        BspCotSenderOutput senderOutput = new BspCotSenderOutput();
        MathPreconditions.checkPositive("num", sspCotSenderOutputs.length);
        // get Δ and each num
        senderOutput.delta = BytesUtils.clone(sspCotSenderOutputs[0].getDelta());
        senderOutput.eachNum = sspCotSenderOutputs[0].getNum();
        // set other outputs
        senderOutput.senderOutputs = Arrays.stream(sspCotSenderOutputs)
            .peek(iOutput -> {
                Preconditions.checkArgument(BytesUtils.equals(senderOutput.delta, iOutput.getDelta()));
                MathPreconditions.checkEqual("each num", "i-th num", senderOutput.eachNum, iOutput.getNum());
            })
            .toArray(SspCotSenderOutput[]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private BspCotSenderOutput() {
        // empty
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets the assigned SSP-COT sender output.
     *
     * @param index index.
     * @return SSP-COT sender output.
     */
    public SspCotSenderOutput get(int index) {
        return senderOutputs[index];
    }

    /**
     * Gets num in each SSP-COT sender output.
     *
     * @return num in each SSP-COT sender output.
     */
    public int getEachNum() {
        return eachNum;
    }

    @Override
    public int getBatchNum() {
        return senderOutputs.length;
    }
}
